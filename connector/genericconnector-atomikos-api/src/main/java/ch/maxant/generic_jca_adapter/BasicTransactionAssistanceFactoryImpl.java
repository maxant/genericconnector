package ch.maxant.generic_jca_adapter;

import javax.resource.ResourceException;
import javax.transaction.Status;
import javax.transaction.Transaction;

import com.atomikos.icatch.jta.UserTransactionManager;

class BasicTransactionAssistanceFactoryImpl implements BasicTransactionAssistanceFactory {

	private String jndiName;

	BasicTransactionAssistanceFactoryImpl(String jndiName) {
		this.jndiName = jndiName;
	}

	@Override
	public TransactionAssistant getTransactionAssistant() throws ResourceException {
		//enlist a new resource into the transaction. it will be delisted, when its closed.
		final CommitRollbackCallback commitRollbackCallback = AtomikosTransactionConfigurator.getCommitRollbackCallback(jndiName);
		MicroserviceXAResource ms = new MicroserviceXAResourceImpl(jndiName, commitRollbackCallback);
		UserTransactionManager utm = new UserTransactionManager();
		try {
			if(utm.getStatus() == Status.STATUS_NO_TRANSACTION){
				throw new ResourceException("no transaction found. please start one before getting the transaction assistant. status was: " + utm.getStatus());
			}
			Transaction tx = utm.getTransaction();
			tx.enlistResource(ms);
			return new AtomikosTransactionAssistantImpl(ms);
		} catch (Exception e) {
			throw new ResourceException("Unable to get transaction status", e);
		}
	}
	
}
