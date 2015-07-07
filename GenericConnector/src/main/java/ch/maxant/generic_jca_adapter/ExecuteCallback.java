package ch.maxant.generic_jca_adapter;


/**
 * Client code should implement this interface in order to 
 * bind a resource into an XA transaction.
 */
public interface ExecuteCallback<O> {

	/** The container calls this method in order
	 * to call a business method on the EIS, 
	 * within a global transaction. */
	O execute(String txid) throws Exception;
	
}
