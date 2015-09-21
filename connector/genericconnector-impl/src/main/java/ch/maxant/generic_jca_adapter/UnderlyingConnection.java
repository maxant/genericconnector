package ch.maxant.generic_jca_adapter;

import javax.resource.ResourceException;

public interface UnderlyingConnection extends CommitRollbackCallback {

	void cleanup() throws ResourceException;

	boolean wasExecuteSuccessful();

	/** only needs to be implemented when recovery is not handled internally */
	String[] getTransactionsInNeedOfRecovery();

	void setCurrentTxId(String s);

}
