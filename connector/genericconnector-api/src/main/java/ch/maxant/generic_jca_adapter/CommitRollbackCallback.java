package ch.maxant.generic_jca_adapter;

import java.io.Serializable;

/**
 * WARNING: the implementation of this class must be thread safe, as multiple threads will call it concurrently!
 */
public interface CommitRollbackCallback extends Serializable {

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
