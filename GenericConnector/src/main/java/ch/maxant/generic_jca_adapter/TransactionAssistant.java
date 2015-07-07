package ch.maxant.generic_jca_adapter;


/** 
 * An interface capable of binding something into a transaction.
 * (The factory creates these.) */
public interface TransactionAssistant extends AutoCloseable {

	/** Submit some work (a function) to be bound into the
	 * currently active transaction. */
	<O> O executeInTransaction(ExecuteCallback<O> tc) throws Exception;

	/** Call before completing the transaction in order 
	 * to free up resources used by the app server. */
    @Override
	void close();
}