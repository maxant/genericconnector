package ch.maxant.generic_jca_adapter;

import java.io.File;
import java.util.logging.Level;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

class MicroserviceResource extends AbstractTransactionAssistanceXAResource {

	private static final long serialVersionUID = 1L;

	/** the microservice that we will be calling, ie one which abstracts an {@link XAResource} like this one. */
	private final CommitRollbackHandler ms;

	/** how old do transactions have to be before we consider cleaning them up? 
	 * since recovery runs async, we need to be careful not to start recovering something thats still active!
	 * TODO could we ask BTM for the active transactions, and skip those ones? */
	private static long minAgeOfTransactionInMSBeforeRelevantForRecovery = 30000L;

	/** where should we store our information about transaction state? */
	private static File recoveryStatePersistenceDirectory = new File(".");

	public MicroserviceResource(CommitRollbackHandler commitRollbackCallback) {
		this.ms = commitRollbackCallback;
	}

	@Override
	protected long getMinAgeOfTransactionBeforeRelevantForRecovery() {
		return minAgeOfTransactionInMSBeforeRelevantForRecovery;
	}
	
	@Override
	protected File getRecoveryStatePersistenceDirectory() {
		return recoveryStatePersistenceDirectory;
	}
	
	@Override
	protected UnderlyingConnection getUnderlyingConnection() {
		return ms;
	}

	@Override
	public boolean isSameRM(XAResource xares) throws XAException {
		log.log(Level.FINEST, "isSameRM " + xares);
		return false;
		// TODO hmm not sure about this
	}

	/** execute the given callback in the active transaction */
	public <T> T executeInActiveTransaction(ExecuteCallback<T> c) throws Exception {
		return ms.execute(c, recoveryStatePersistenceDirectory);
	}

	public static void configure(long minAgeOfTransactionInMSBeforeRelevantForRecovery, File recoveryStatePersistenceDirectory){
		MicroserviceResource.minAgeOfTransactionInMSBeforeRelevantForRecovery = minAgeOfTransactionInMSBeforeRelevantForRecovery;
		MicroserviceResource.recoveryStatePersistenceDirectory = recoveryStatePersistenceDirectory;
	}
}
