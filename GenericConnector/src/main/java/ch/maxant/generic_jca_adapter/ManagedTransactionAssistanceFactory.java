package ch.maxant.generic_jca_adapter;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

import ch.maxant.generic_jca_adapter.TransactionAssistanceFactory.CommitRollbackRecoveryCallback;

/**
 * A class which the container uses to create "connection" factories. It contains the info that the container needs.
 * The client doesn't use this class. This class is referenced in the module/resource adapter declaration (xml).
 * It is associated with the resource adapter.
 */
@ConnectionDefinition(connectionFactory = TransactionAssistanceFactory.class, //the thing injected into say a servlet/EJB
    connectionFactoryImpl = TransactionAssistanceFactoryImpl.class, //impl of the factory
    connection = TransactionAssistant.class, //the connection interface to the EIS, exposing its business interface. the factory creates these.
    connectionImpl = TransactionAssistantImpl.class //the impl of the business interface
)
public class ManagedTransactionAssistanceFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {

	private static final long serialVersionUID = 1L;

	private final Logger log = Logger.getLogger(this.getClass().getName());

	private PrintWriter logWriter;
	
    private ResourceAdapter resourceAdapter;

    @ConfigProperty(supportsDynamicUpdates=false, defaultValue="false")
    private String handleRecoveryInternally;

    @ConfigProperty(supportsDynamicUpdates=false, defaultValue="")
    private String recoveryStatePersistenceDirectory;
    
    @ConfigProperty(supportsDynamicUpdates=false)
    private String id;

	private File recoveryStatePersistenceDirectoryFile;
    
	private boolean initialised = false;
	
    private synchronized void lazyInit(){
    	if(initialised) return;
		if(isHandleRecoveryInternally()){
			if(recoveryStatePersistenceDirectory == null || recoveryStatePersistenceDirectory.isEmpty()){
				log.log(Level.SEVERE, "The '" + id + "' adapter has been configured to handle recovery state internally, but the configuration property 'recoveryStatePersistenceDirectoryConfig' has not been set!");
			}else{
				log.log(Level.WARNING, "The '" + id + "' adapter WILL track transaction state internally. The associated EIS does NOT need to be able to return incomplete transactions and there is NO need to provide an implementation of CommitRollbackRecoveryCallback#getTransactionsInNeedOfRecovery().");
				recoveryStatePersistenceDirectoryFile = new File(recoveryStatePersistenceDirectory);
				if(!recoveryStatePersistenceDirectoryFile.exists()){
					if(!recoveryStatePersistenceDirectoryFile.mkdirs()){
						String msg = "FAILED TO CREATE DIRECTORY '" + recoveryStatePersistenceDirectoryFile.getAbsolutePath() + "' - the resource adapter will be unable to track state. Throwing exception now...";
						log.log(Level.SEVERE, msg);
						throw new RuntimeException(msg);
					}else{
						log.log(Level.INFO, "Transaction state for '" + id + "' will be written in new directory '" + recoveryStatePersistenceDirectoryFile.getAbsolutePath() + "'");
					}
				}else{
					log.log(Level.INFO, "Transaction state for '" + id + "' will be written in existing directory '" + recoveryStatePersistenceDirectoryFile.getAbsolutePath() + "'");
				}
			}
		}else{
			log.log(Level.WARNING, "The '" + id + "' adapter will NOT track transaction state internally. The associated EIS MUST be able to return incomplete transactions and you MUST provide an implementation of CommitRollbackRecoveryCallback#getTransactionsInNeedOfRecovery()!");
		}
		initialised = true;
	}
    
    /**
     * Creates a Connection Factory instance.
     *
     * @return EIS-specific Connection Factory instance or
     * javax.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    @Override
	public Object createConnectionFactory() throws ResourceException {
    	lazyInit();
        throw new ResourceException("This resource adapter doesn't support non-managed environments");
    }

    /**
     * Probably called by container to get the factory which it injects into e.g. servlets.
     * 
     * Creates a Connection Factory instance.
     *
     * @param cxManager ConnectionManager to be associated with created EIS
     * connection factory instance
     * @return EIS-specific Connection Factory instance or
     * javax.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    @Override
	public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
    	lazyInit();
        return new TransactionAssistanceFactoryImpl(this, cxManager);
    }

    /**
     * Creates a new physical connection to the underlying EIS resource manager.
     *
     * @param subject Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection
     * request information
     * * @throws ResourceException generic exception
     * @return ManagedConnection instance
     */
    @Override
	public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
    	lazyInit();
        CommitRollbackRecoveryCallback callback = ((GenericResourceAdapter)this.resourceAdapter).getCommitRollbackRecoveryCallback(id);
		return new ManagedTransactionAssistance(callback, isHandleRecoveryInternally(), recoveryStatePersistenceDirectoryFile, id);
    }

	private Boolean isHandleRecoveryInternally() {
		return Boolean.valueOf(handleRecoveryInternally);
	}

    /**
     * For pooling support.
     * 
     * Returns a matched connection from the candidate set of connections.
     *
     * @param connectionSet Candidate connection set
     * @param subject Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection request information
     * @throws ResourceException generic exception
     * @return ManagedConnection if resource adapter finds an acceptable match otherwise null
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
    	lazyInit();
    	ManagedConnection result = null;
    	Iterator<ManagedConnection> it = connectionSet.iterator();
        while (result == null && it.hasNext()) {
        	ManagedConnection mc = it.next();
            if (mc instanceof ManagedTransactionAssistance) {
                result = mc;
            }
        }
        return result;
    }

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		return logWriter;
	}
	
	@Override
	public void setLogWriter(PrintWriter logWriter) throws ResourceException {
		this.logWriter = logWriter;
	}
 
	@Override
	public ResourceAdapter getResourceAdapter() {
		return resourceAdapter;
	}
	
	@Override
	public void setResourceAdapter(ResourceAdapter resourceAdapter)
			throws ResourceException {
		this.resourceAdapter = resourceAdapter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ManagedTransactionAssistanceFactory other = (ManagedTransactionAssistanceFactory) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public void setHandleRecoveryInternally(
			String handleRecoveryInternally) {
		this.handleRecoveryInternally = handleRecoveryInternally;
	}
	
	public void setRecoveryStatePersistenceDirectory(
			String recoveryStatePersistenceDirectory) {
		this.recoveryStatePersistenceDirectory = recoveryStatePersistenceDirectory;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
}