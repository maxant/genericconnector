package ch.maxant.generic_jca_adapter;

import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.atomikos.datasource.RecoverableResource;
import com.atomikos.datasource.ResourceException;
import com.atomikos.icatch.Participant;
import com.atomikos.icatch.RecoveryService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;

public class TestAtomikos {

	public static void main(String[] args) throws Exception {
		
		UserTransactionServiceImp utsi = new UserTransactionServiceImp();
		utsi.registerResource(new RecoverableResource() {
			
			@Override
			public void setRecoveryService(RecoveryService arg0) throws ResourceException {
				System.out.println(); //TODO
			}
			
			@Override
			public boolean recover(Participant arg0) throws ResourceException {
				System.out.println();
				return false; //TODO
			}
			
			@Override
			public boolean isSameRM(RecoverableResource arg0) throws ResourceException {
				System.out.println();
				return false; //TODO
			}
			
			@Override
			public boolean isClosed() {
				System.out.println();
				return false; //TODO
			}
			
			@Override
			public String getName() {
				System.out.println();
				return "xa/ms1"; //TODO
			}
			
			@Override
			public void endRecovery() throws ResourceException {
				System.out.println(); //TODO
			}
			
			@Override
			public void close() throws ResourceException {
				System.out.println(); //TODO
			}
		});
		
//        TSInitInfo info = utsi.createTSInitInfo();
        //TODO optionally set config properties on info?
        utsi.init();//info);
		
		UserTransactionManager utm = new UserTransactionManager();

		utm.begin();
		
        Transaction tx = utm.getTransaction();
        MicroserviceResource ms = new MicroserviceResource(new CommitRollbackHandler() {
			@Override
			public void rollback(String txid) throws Exception {
				System.out.println(); //TODO
			}
			@Override
			public void commit(String txid) throws Exception {
				System.out.println(); //TODO
			}
		});
		tx.enlistResource(ms);

		//call microservice
		String result = ms.executeInActiveTransaction(new ExecuteCallback<String>() {
			@Override
			public String execute(String txid) throws Exception {
				return "executing " + txid;
			}
		});
		System.out.println(result);

		//do we need to delist??
		if(true){
			tx.delistResource(ms, XAResource.TMSUCCESS);
			utm.commit();
		}else{
			tx.delistResource(ms, XAResource.TMFAIL);
			utm.rollback();
		}
		
		utsi.shutdown(false);
	}
}
