package ch.maxant.generic_jca_adapter;

class MicroserviceXAResourceImpl extends MicroserviceXAResource {
	private static final long serialVersionUID = 1L;
	public MicroserviceXAResourceImpl(String jndiName, final CommitRollbackCallback commitRollbackCallback) {
		super(jndiName, new UnderlyingConnectionImpl() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
				commitRollbackCallback.rollback(txid);
			}
			@Override
			public void commit(String txid) throws Exception {
				commitRollbackCallback.commit(txid);
			}
		});
	}
}
