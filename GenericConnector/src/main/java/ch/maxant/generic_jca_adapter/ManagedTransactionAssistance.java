package ch.maxant.generic_jca_adapter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import ch.maxant.generic_jca_adapter.TransactionAssistanceFactory.CommitRollbackRecoveryCallback;

/**
 * There is an assumption here that instances of this class are not thread safe
 * in that they can only be used one at a time.  EXECUTE-COMMIT or EXECUTE-ROLLBACK.
 * The class contains logic to ensure this.
 */
public class ManagedTransactionAssistance implements ManagedConnection, Serializable {

	private static final long serialVersionUID = 1L;

	private final Logger log = Logger.getLogger(this.getClass().getName());

    private PrintWriter logWriter;

    private List<ConnectionEventListener> listeners = new ArrayList<>();

    private TransactionAssistant connection;

    /** was the call to EXECUTE successful? (tristate, null means ready for new connection) */
    private Boolean executeWasSuccessful;

    /** the current transaction ID */
    private String currentTxId;

	private CommitRollbackRecoveryCallback commitRollbackRecoveryCallback;

	//spec 1.6: 7.3.2.1: "A resource adapter is responsible for maintaining a 1-1 relationship between the
	//ManagedConnection and XAResource instances. Each time a
	//ManagedConnection.getXAResource method is called, the same XAResource
	//instance has to be returned."
	private TransactionAssistanceXAResource xa;

	/** should the resource adapter track state for recovery, or will the remote system be able to tell us about transactions that require recovery? */
	private boolean handleRecoveryInternally;

	/** if {@link #handleRecoveryInternally} is true, then this is the directory where to persist state. */
	private File recoveryStatePersistenceDirectory;

	/** managedConnectionFactoryId from the managed connection factory */
	private String managedConnectionFactoryId;

	public ManagedTransactionAssistance(CommitRollbackRecoveryCallback commitRollbackRecoveryCallback, boolean handleRecoveryInternally, File recoveryStatePersistenceDirectory, String managedConnectionFactoryId) {
		this.commitRollbackRecoveryCallback = commitRollbackRecoveryCallback;
		this.handleRecoveryInternally = handleRecoveryInternally;
		this.recoveryStatePersistenceDirectory = recoveryStatePersistenceDirectory;
		this.managedConnectionFactoryId = managedConnectionFactoryId;
    	this.xa = new TransactionAssistanceXAResource(this);
    	this.connection = new TransactionAssistantImpl(this);
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
	public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
		//during recovery, currentTxId and executeWasSuccessful are unknown, so we cannot check them like this:
    	//if(currentTxId == null) throw new IllegalStateException("transaction not started?");
		//if(executeWasSuccessful != null) throw new IllegalStateException("did you forget to call close?");

        return connection;
    }

    /**
     * Used by the container to change the association of an
     * application-level connection handle with a ManagedConneciton instance.
     *
     * @param connection Application-level connection handle
     * @throws ResourceException generic exception if operation fails
     */

    @Override
	public void associateConnection(Object connection) throws ResourceException {
    	//never seen this called in JBoss!
        if (!(connection instanceof TransactionAssistant)) throw new IllegalArgumentException("Connection must be of type TransactionAssistant instead of " + connection.getClass());
        this.connection = (TransactionAssistant) connection;
    }

	@Override
    public void cleanup() throws ResourceException {
        log.log(Level.INFO, "cleaning up managed connection");

        currentTxId = null;
        executeWasSuccessful = null;
    }

    @Override
    public void destroy() throws ResourceException {
        this.connection = null;
        this.xa = null;
        currentTxId = null;
        executeWasSuccessful = null;
    }

    @Override
	public void addConnectionEventListener(ConnectionEventListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener is null");
        listeners.add(listener);
    }

    @Override
	public void removeConnectionEventListener(ConnectionEventListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener is null");
        listeners.remove(listener);
    }

	@Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("LocalTransaction not supported");
    }
    
    @Override
    public XAResource getXAResource() throws ResourceException {
        return xa;
    }

	@Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new ManagedTransactionAssistanceMetaData();
    }

	/** Calls the callback to do some work bound into the transaction. 
	 * Tracks transaction state internally if necessary. */
	public <O> O execute(ExecuteCallback<O> f) throws Exception {
		if(currentTxId == null) throw new IllegalStateException("XID not yet set - was transaction started?");
		if(executeWasSuccessful != null) throw new IllegalStateException("not closed?");
		
		if(handleRecoveryInternally){
			persistTransactionState();
		}
		
        try{
        	O o = f.execute(currentTxId);
            executeWasSuccessful = true;
            return o;
        }catch(Exception e) {
            executeWasSuccessful = false;
            throw e;
        }
    }

	private void persistTransactionState() throws IOException {
		Files.write(File.createTempFile("exec.", ".txt", getRecoveryStatePersistenceDirectory()).toPath(), currentTxId.getBytes(StandardCharsets.UTF_8));
	}

    public void close(TransactionAssistant handle) {
    	ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(handle);
        for(ConnectionEventListener l : listeners){
            l.connectionClosed(event);
        }
    }

    /** 
     * Was the call to the execute method successful? 
     * Used by the XAResource to determine if PREPARE can
     * return OK or NOK.
     * 
     * @throws IllegalStateException if the call to execute has not yet occurred
     */
    public boolean wasExecuteSuccessful() {
        if(this.executeWasSuccessful == null){
            throw new IllegalStateException("not expecting a call to wasExecuteSuccessful at this time");
        }else{
            return executeWasSuccessful;
        }
    }
    
    public void setCurrentTxId(String txId){
        if(currentTxId != null) throw new IllegalStateException("not ready for a new transaction - was this connection closed?");
        this.currentTxId = txId;
    }

    public CommitRollbackRecoveryCallback getCommitRollbackRecoveryCallback() {
		return commitRollbackRecoveryCallback;
	}
    
    public boolean isHandleRecoveryInternally() {
		return handleRecoveryInternally;
	}
    
    public File getRecoveryStatePersistenceDirectory() {
		return recoveryStatePersistenceDirectory;
	}
    
    /** the managedConnectionFactoryId of the managed connection factory */
    public String getManagedConnectionFactoryId() {
		return managedConnectionFactoryId;
	}
}
