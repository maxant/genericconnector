package ch.maxant.generic_jca_adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.TransactionSupport.TransactionSupportLevel;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import ch.maxant.generic_jca_adapter.TransactionAssistanceFactory.CommitRollbackRecoveryCallback;

/** A resource adapter able to bind callbacks into XA */
@Connector(licenseRequired=true, transactionSupport = TransactionSupportLevel.XATransaction)
public class GenericResourceAdapter implements ResourceAdapter {

    private final Logger log = Logger.getLogger(this.getClass().getName());

    private Map<String, CommitRollbackRecoveryCallback> commitRollbackRecoveryCallbacks = new HashMap<>();
    
    @Override
	public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        log.log(Level.INFO, "activating endpoint");
    }

    @Override
	public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        log.log(Level.INFO, "deactivating endpoint");
    }

    @Override
	public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        log.log(Level.INFO, "starting resource adapter");
    }

    @Override
	public void stop() {
        log.log(Level.INFO, "stopping resource adapter");
    }

    @Override
	public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        log.log(Level.INFO, "getting xa resources");
        //TODO never seen this method called by Jboss! therefore:
        throw new ResourceException("not supported - altho we can if its necessary...");
        //TODO is returning one enough?
        //return new XAResource[]{new TransactionAssistanceXAResource(new ManagedTransactionAssistance())};
    }

    @Override
	public int hashCode() {
    	return 69;
	}

    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

    /** client code can register a callback to enable commit, rollback and recovery */
    synchronized void registerCommitRollbackRecovery(String managedConnectionFactoryId, CommitRollbackRecoveryCallback commitRollbackRecoveryCallback) {
		if(this.commitRollbackRecoveryCallbacks.containsKey(managedConnectionFactoryId)){
			throw new IllegalStateException("Unable to register commit/rollback/recovery for managed connection factory with ID '" + managedConnectionFactoryId + "', because a callback has already been registered. Please unregister it first!");
		}
		this.commitRollbackRecoveryCallbacks.put(managedConnectionFactoryId, commitRollbackRecoveryCallback);
	}
	
    /** client code can unregister the callback which was registered using {@link #registerCommitRollbackRecovery(String, CommitRollbackRecoveryCallback)} */
    synchronized void unregisterCommitRollbackRecovery(String managedConnectionFactoryId){
		this.commitRollbackRecoveryCallbacks.remove(managedConnectionFactoryId);
	}

    /** get the callback registered using {@link #registerCommitRollbackRecovery(String, CommitRollbackRecoveryCallback)} */
	CommitRollbackRecoveryCallback getCommitRollbackRecoveryCallback(String managedConnectionFactoryId) {
		return commitRollbackRecoveryCallbacks.get(managedConnectionFactoryId);
	}
}