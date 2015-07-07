package ch.maxant.generic_jca_adapter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

public class TransactionAssistanceFactoryImpl implements TransactionAssistanceFactory {

	private static final long serialVersionUID = 1L;

	private final Logger log = Logger.getLogger(this.getClass().getName());
    
    private Reference reference;

	private ManagedTransactionAssistanceFactory mcf;

	private ConnectionManager cxManager;

    public TransactionAssistanceFactoryImpl(ManagedTransactionAssistanceFactory mcf, ConnectionManager cxManager) {
		this.mcf = mcf;
		this.cxManager = cxManager;
    }

    @Override
	public TransactionAssistant getTransactionAssistant() throws ResourceException {
        log.log(Level.INFO, "allocating connection");
        return (TransactionAssistant) cxManager.allocateConnection(mcf, null);
    }
    
    @Override
    public void registerCommitRollbackRecovery(
    		CommitRollbackRecoveryCallback commitRollbackRecoveryCallback) {
    	((GenericResourceAdapter)this.mcf.getResourceAdapter()).registerCommitRollbackRecovery(mcf.getId(), commitRollbackRecoveryCallback);
    }
    
    @Override
    public void unregisterCommitRollbackRecovery() {
    	((GenericResourceAdapter)this.mcf.getResourceAdapter()).unregisterCommitRollbackRecovery(mcf.getId());
    }
    
    @Override
	public Reference getReference() {
		return reference;
	}
    
    @Override
	public void setReference(Reference reference) {
		this.reference = reference;
	}

}