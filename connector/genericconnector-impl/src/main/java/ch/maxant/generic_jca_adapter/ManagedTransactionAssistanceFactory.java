/*
   Copyright 2015 Ant Kutschera

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
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
    
    @Override
    public Object createConnectionFactory() throws ResourceException {
        lazyInit();
        throw new ResourceException("This resource adapter doesn't support non-managed environments");
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        lazyInit();
        return new TransactionAssistanceFactoryImpl(this, cxManager);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        lazyInit();
        CommitRollbackRecoveryCallback callback = ((GenericResourceAdapter)this.resourceAdapter).getCommitRollbackRecoveryCallback(id);
        return new ManagedTransactionAssistance(callback, isHandleRecoveryInternally(), recoveryStatePersistenceDirectoryFile, id);
    }

    private Boolean isHandleRecoveryInternally() {
        return Boolean.valueOf(handleRecoveryInternally);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        lazyInit();
        ManagedConnection result = null;
        Iterator<ManagedConnection> it = connectionSet.iterator();
        while (result == null && it.hasNext()) {
            ManagedConnection mc = it.next();
            if (mc instanceof ManagedTransactionAssistance) {
            	if(cxRequestInfo != null /*&& TODO future: it contains the ID*/){
            		ManagedTransactionAssistance mta = (ManagedTransactionAssistance)mc;
            		/*
            		if(cxRequestInfo.requiredMCFId().equals(mta.getManagedConnectionFactoryId()){
            			result = mc;
        			}
            		*/
            	}
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
