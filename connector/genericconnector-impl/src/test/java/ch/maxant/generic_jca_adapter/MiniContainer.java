package ch.maxant.generic_jca_adapter;

import java.util.ArrayList;
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
		RUNNING, COMMITTING, ROLLINGBACK, PREPARING, RECOVERING;
	}

	public static class Transaction {
		private TransactionState transactionState = TransactionState.RUNNING;
		private ArrayList<XAResource> xaResources = new ArrayList<XAResource>();
		private Xid xid = new XidImpl("gtid".getBytes(), transactionNumber.getAndIncrement(), "bq".getBytes());
	}
	
	private GenericResourceAdapter adapter = new GenericResourceAdapter();
	private ManagedTransactionAssistanceFactory mtaf;
	private ConnectionManager cm;
	private Transaction tx = null;

	public MiniContainer() throws ResourceException {
		adapter.start(null); //TODO parameters
		adapter.endpointActivation(null, null); //TODO parameters
		
		mtaf = new ManagedTransactionAssistanceFactory();
		mtaf.setHandleRecoveryInternally("true");
		mtaf.setRecoveryStatePersistenceDirectory(System.getProperty("java.io.tmpdir"));
		mtaf.setResourceAdapter(adapter);
		mtaf.setId("A");

		cm = new ConnectionManager() {
			private static final long serialVersionUID = 1L;
			@Override
			public Object allocateConnection(ManagedConnectionFactory arg0,
					ConnectionRequestInfo arg1) throws ResourceException {

				ManagedTransactionAssistance mta = (ManagedTransactionAssistance) mtaf.createManagedConnection(null, null);

				if(tx == null) throw new IllegalStateException("please start a transaction before opening a connection");
				
				XAResource xa = mta.getXAResource();
				tx.xaResources.add(xa);
				try {
					xa.start(tx.xid, 0);
				} catch (XAException e) {
					e.printStackTrace();
					throw new ResourceException(e);
				}
				
				return new TransactionAssistantImpl(mta);
			}
		};
	}
	
	public TransactionAssistanceFactory lookupAdapter() throws InstantiationException, IllegalAccessException, ResourceException {
		return (TransactionAssistanceFactory) mtaf.createConnectionFactory(cm);
	}

	public Transaction startTransaction() throws ResourceException {
		if(tx != null){
			throw new IllegalStateException("i dont yet support multiple transactions, and one was already started!");
		}
		tx = new Transaction();
		return tx;
	}
	
	public TransactionState getTransactionState(Transaction transaction) {
		if(tx != null && transaction.xid.equals(tx.xid)){
			return tx.transactionState;
		}
		return null;
	}

	public void rollback(Transaction tx) throws XAException {
		tx.transactionState = TransactionState.ROLLINGBACK;

		for(XAResource xa : tx.xaResources){
			xa.rollback(tx.xid);
		}
		
		tx = null;
	}

	public void commit(Transaction tx) throws XAException {
		if(tx.transactionState != TransactionState.RUNNING) throw new IllegalStateException("was not running, rather was " + tx.transactionState);
		tx.transactionState = TransactionState.PREPARING;

		boolean commit = true;
		for(XAResource xa : tx.xaResources){
			try{
				xa.prepare(tx.xid);
			}catch(XAException e){
				if(e.errorCode == XAException.XA_RBROLLBACK){
					commit = false;
					break;
				}
			}
		}
		
		if(commit){
			tx.transactionState = TransactionState.COMMITTING;
		}else{
			tx.transactionState = TransactionState.ROLLINGBACK;
		}
		
		for(XAResource xa : tx.xaResources){
			if(commit){
				xa.commit(tx.xid, false);
			}else{
				xa.rollback(tx.xid);
			}
		}
		
		tx = null;
	}

}
