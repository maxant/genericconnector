package ch.maxant.generic_jca_adapter;

public interface CommitRollbackCallback {

    /** The container will call this function 
     * to commit a transaction that was successful.
     * The implementation of this method should
     * call the EIS in order to commit
     * the transaction. */
    void commit(String txid) throws Exception;
    
    /** The container will call this function 
     * to rollback an unsuccessful transaction.
     * The implementation of this method should
     * call the EIS in order to rollback
     * the transaction. */
    void rollback(String txid) throws Exception;
	
}
