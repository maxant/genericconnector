package ch.maxant.generic_jca_adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class MiniContainer {

	private static AtomicInteger transactionNumber = new AtomicInteger();
	
	public static enum TransactionState {
		RUNNING, PREPARING, COMMITTING, ROLLINGBACK;
	}

	public static class Transaction {
		private TransactionState transactionState = TransactionState.RUNNING;
		private ArrayList<XAResource> xaResources = new ArrayList<XAResource>();
		private Xid xid = new XidImpl("gtid".getBytes(), transactionNumber.getAndIncrement(), "bq".getBytes());
		private boolean shouldRollback;
		public List<Exception> exceptions = new ArrayList<Exception>();
		
		public TransactionState getTransactionState() {
			return transactionState;
		}
		public boolean isShouldRollback() {
			return shouldRollback;
		}
		public String getTxid() {
			return XidImpl.asString(xid);
		}
		public List<Exception> getExceptions() {
			return exceptions;
		}
	}
	
	private GenericResourceAdapter adapter = new GenericResourceAdapter();
	private ManagedTransactionAssistanceFactory mtaf;
	private ConnectionManager cm;
	private ThreadLocal<Transaction> tx = new ThreadLocal<MiniContainer.Transaction>();
	private List<Transaction> allKnownTransactions = new ArrayList<Transaction>();
	
	public MiniContainer() throws ResourceException {
		this(true, "0"); //so that tests run quick, do this immediately
	}

	/** sets up the resource adapter and associated classes in the same way that the real container does it */
	public MiniContainer(boolean handleRecoveryInternally, String minAgeOfTransactionBeforeRelevantForRecovery) throws ResourceException {
		adapter.start(null); //TODO parameters
		adapter.endpointActivation(null, null); //TODO parameters
		
		mtaf = new ManagedTransactionAssistanceFactory();
		if(handleRecoveryInternally){
			mtaf.setHandleRecoveryInternally("true");
			mtaf.setRecoveryStatePersistenceDirectory(System.getProperty("java.io.tmpdir"));
		}else{
			mtaf.setHandleRecoveryInternally("false");
		}
		mtaf.setMinAgeOfTransactionBeforeRelevantForRecovery(minAgeOfTransactionBeforeRelevantForRecovery);
		mtaf.setResourceAdapter(adapter);
		mtaf.setId("A");

		cm = new ConnectionManager() {
			private static final long serialVersionUID = 1L;
			@Override
			public Object allocateConnection(ManagedConnectionFactory arg0,
					ConnectionRequestInfo arg1) throws ResourceException {

				if(tx.get() == null) throw new IllegalStateException("please start a transaction before opening a connection");

				ManagedTransactionAssistance mta = (ManagedTransactionAssistance) mtaf.createManagedConnection(null, null);
				
				XAResource xa = mta.getXAResource();
				tx.get().xaResources.add(xa);
				try {
					xa.start(tx.get().xid, 0);
				} catch (XAException e) {
					e.printStackTrace();
					throw new ResourceException(e);
				}
				
				return new TransactionAssistantImpl(mta);
			}
		};
	}
	
	/** like a JNDI lookup in the real world */
	public TransactionAssistanceFactory lookupAdapter() throws InstantiationException, IllegalAccessException, ResourceException {
		return (TransactionAssistanceFactory) mtaf.createConnectionFactory(cm);
	}

	/** tells the container to start a transaction */
	public Transaction startTransaction() throws ResourceException {
		if(tx.get() != null){
			throw new IllegalStateException("i dont yet support multiple transactions, and one was already started!");
		}
		tx.set(new Transaction());
		allKnownTransactions.add(tx.get());
		return tx.get();
	}
	
	/** tells the container to rollback */
	public void setRollback() throws XAException {
		if(tx.get() == null) throw new IllegalStateException("not in transaction");
		tx.get().shouldRollback = true;
	}

	/** called at the end of your business logic when the transaction should be ended. similar to what the container does in real life */
	public void finishTransaction() throws XAException {
		if(tx.get() == null) throw new IllegalStateException("not in a transaction");
		if(tx.get().transactionState != TransactionState.RUNNING) throw new IllegalStateException("was not running, rather was " + tx.get().transactionState);
		finishTx(tx.get());
		tx.set(null);
	}
	
	private void finishTx(Transaction transaction) throws XAException {
		transaction.transactionState = TransactionState.PREPARING;

		boolean commit = true;
		if(transaction.shouldRollback){
			commit = false;
		}
		for(XAResource xa : transaction.xaResources){
			xa.end(transaction.xid, 0);
			try{
				xa.prepare(transaction.xid);
			}catch(XAException e){
				if(e.errorCode == XAException.XA_RBROLLBACK){
					commit = false;
				}
			}
		}
		
		if(commit){
			transaction.transactionState = TransactionState.COMMITTING;
		}else{
			transaction.transactionState = TransactionState.ROLLINGBACK;
		}
		
		for(XAResource xa : transaction.xaResources){
			try{
				if(commit){
					xa.commit(transaction.xid, false);
				}else{
					xa.rollback(transaction.xid);
				}
			}catch(XAException e){
				transaction.exceptions.add(e);
			}
		}
	}

	public List<String> recover() throws XAException, InterruptedException {
		List<String> txids = new ArrayList<String>();
		for(Transaction transaction : allKnownTransactions){
			for(XAResource xa : transaction.xaResources){
				Xid[] xids = xa.recover(XAResource.TMSTARTRSCAN);
				for(Xid xid : xids){
					txids.add(XidImpl.asString(xid));
				}
			}

			//now do recovery
			//TODO actually we'd want to do this for all known transactions (threads)?! but not important here.
			finishTx(transaction);
		}
		
		return txids;
	}

}
