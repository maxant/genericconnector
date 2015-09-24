package ch.maxant.generic_jca_adapter;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.atomikos.icatch.jta.UserTransactionManager;

public class AtomikosTransactionAssistantImpl implements TransactionAssistant {

	private MicroserviceResource ms;

	public AtomikosTransactionAssistantImpl(MicroserviceResource ms) {
		this.ms = ms;
	}

	@Override
	public <O> O executeInActiveTransaction(ExecuteCallback<O> c) throws Exception {
		return ms.executeInActiveTransaction(c);
	}

	@Override
	public void close() {
		UserTransactionManager utm = new UserTransactionManager();
		try {
			if(utm.getStatus() == Status.STATUS_NO_TRANSACTION){
				throw new RuntimeException("no transaction found. please start one before getting the transaction assistant. status was: " + utm.getStatus());
			}
			Transaction tx = utm.getTransaction();
			tx.delistResource(ms, ms.getUnderlyingConnection().wasExecuteSuccessful() ? XAResource.TMSUCCESS : XAResource.TMFAIL);
		} catch (Exception e) {
			throw new RuntimeException("Unable to delist resource from transaction", e);
		}
	}

}
