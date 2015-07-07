package ch.maxant.generic_jca_adapter;

public class TransactionAssistantImpl implements TransactionAssistant {

	private ManagedTransactionAssistance mc;

	public TransactionAssistantImpl(ManagedTransactionAssistance mc) {
		this.mc = mc;
	}
	
	@Override
	public <O> O executeInTransaction(ExecuteCallback<O> tc) throws Exception {
		return mc.execute(tc);
	}
	
    @Override
	public void close() {
        mc.close(this);
    }

}