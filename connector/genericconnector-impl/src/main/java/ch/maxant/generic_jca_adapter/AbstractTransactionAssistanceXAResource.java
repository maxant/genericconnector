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
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * {@link XAResource} used by the transaction assistance JCA Adapter to bind callbacks 
 * into transactions.
 */
abstract class AbstractTransactionAssistanceXAResource implements XAResource, Serializable {

    private static final long serialVersionUID = 1L;
    
    /** If tracking state internally, then this filter finds 
     * files related to incomplete transactions */
    protected static FileFilter executeFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().toLowerCase().startsWith("exec.");
        }
    };

    protected static final Logger log = Logger.getLogger(AbstractTransactionAssistanceXAResource.class.getName());

    /** default timeout, as well as that set by the system */
    protected int timeout = 300;
    
    protected abstract UnderlyingConnection getUnderlyingConnection();

    protected boolean isHandleRecoveryInternally() {
		return true;
	}

    protected abstract long getMinAgeOfTransactionBeforeRelevantForRecovery();

    protected abstract File getRecoveryStatePersistenceDirectory();

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        String s = XidImpl.asString(xid);
        log.log(Level.INFO, "COMMIT " + onePhase + "/" + s);

        //regardless of onePhase, we need to tell the external system that we are done and 
        //the previous call to the EXECUTE method should be COMMITTED.
        try{
        	getUnderlyingConnection().commit(s);

            if(isHandleRecoveryInternally()){
                cleanupInternalTransactionState(xid);
            }
            
            getUnderlyingConnection().cleanup();

        }catch(Exception e){
            log.log(Level.SEVERE, "Failed to COMMIT", e);
            int var = XAException.XA_RETRY;
            throw new XAException(var);
        }
    }

	@Override
    public void end(Xid xid, int flags) throws XAException {
        
        String s = "-";
        if(flags == TMSUSPEND){
            s = "TMSUSPEND";
        }else if(flags == TMFAIL){
            s = "TMFAIL";
        }else if(flags == TMSUCCESS){
            s = "TMSUCCESS";
        }
        
        log.log(Level.INFO, "END flags=" + s + "(" + flags + ")" + "/" + XidImpl.asString(xid));
        
        //there is nothing to do at this stage, it is purely informational
    }

    @Override
    public void forget(Xid xid) throws XAException {
        log.log(Level.INFO, "FORGET " + XidImpl.asString(xid));

        if(isHandleRecoveryInternally()){
            cleanupInternalTransactionState(xid);
        }else{
            //TODO send this to the callback?
        }
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        // TODO from config?
        return timeout;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        log.log(Level.INFO, "PREPARE " + XidImpl.asString(xid));

        //if the EXECUTE was unsuccessful, we must not let a COMMIT occur, since it will fail

        //7.6.2.8 of the JCA spec 1.6 says we must not erase knowledge 
        //of the transaction branch until commit or rollback is called.
        // => do not call cleanup yet!
        
        if(!getUnderlyingConnection().wasExecuteSuccessful()){
            //vote to rollback :o(
            throw new XAException(XAException.XA_RBROLLBACK);
        }else{
            //a successful execute is a guarantee that we can commit => therefore vote "OK to commit"
            return XAResource.XA_OK;
        }
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        List<Xid> xids = new ArrayList<Xid>();
        
        switch(flag) {
        case (XAResource.TMSTARTRSCAN):
            log.log(Level.INFO, "RECOVER TMSTARTRSCAN");
            UnderlyingConnection callback = getUnderlyingConnection();
            if(callback != null){
                final String[] unfinishedTxIds;
                if(isHandleRecoveryInternally()){
                    unfinishedTxIds = getTransactionsInNeedOfRecovery();
                }else{
                    unfinishedTxIds = callback.getTransactionsInNeedOfRecovery();
                }
                if(unfinishedTxIds != null){
                    for(String txId : unfinishedTxIds){
                        log.log(Level.INFO, "recovery required for " + txId);
                        Xid xid = XidImpl.getXid(txId);
                        xids.add(xid);
                    }
                }
            }
            break;
        case (XAResource.TMENDRSCAN):
            log.log(Level.INFO, "RECOVER TMENDRSCAN");
            break;
        case (XAResource.TMNOFLAGS):
            log.log(Level.INFO, "RECOVER TMNOFLAGS");
            break;
        default:
            log.log(Level.INFO, "RECOVER " + flag);
        }

        return xids.toArray(new Xid[0]);
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        String s = XidImpl.asString(xid);
        log.log(Level.INFO, "ROLLBACK " + s);
        
        try{
        	getUnderlyingConnection().rollback(s);
            
            if(isHandleRecoveryInternally()){
                cleanupInternalTransactionState(xid);
            }

            getUnderlyingConnection().cleanup();
        }catch(Exception e){
            log.log(Level.WARNING, "failed to rollback for txid " + s, e);
            throw new XAException(XAException.XA_RETRY);
        }
    }

    private void cleanupInternalTransactionState(Xid xid) {
        boolean found = false;
        for(File f : getRecoveryStatePersistenceDirectory().listFiles(executeFilter)){
            try{
                final String content = read(f);
                if(content.equals(XidImpl.asString(xid))){
                    found = true;
                    if(!f.delete()){
                        log.log(Level.WARNING, "Failed to delete file '" + f.getAbsolutePath() + "'. Please do this manually!");
                    }else{
                        log.log(Level.FINE, "Transaction cleaned up: " + f.getName());
                    }
                    break;
                }
            }catch(NoSuchFileException e){
                //this can happen during concurrent attempts to commit/rollback.
                //the concurrent attempts are not trying to find the same file, 
                //rather the one XAResource has simply cleaned up a file by the 
                //time the second XAResource tries to read it, since its 
                //list of files has become outdated.  since another XAResource
                //has already tidied up the file, it did not contain 
                //the Xid we are searching for, so we do nothing, just as though
                //we had successfully read the file and it had not been relevant
                //to the transaction we are searching for.
                //summed up: move along, nothing to see here!
            }
        }
        if(!found){
            log.log(Level.WARNING, "Unable to clean up internal state for transaction '" + xid + "' (" + XidImpl.asString(xid) + ") because no record of the transaction was found. Please report this as a bug.");
        }
    }

	private String read(File f) throws NoSuchFileException {
        try{
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        }catch(NoSuchFileException e){
            throw e;
        }catch(IOException e){
            throw new RuntimeException("failed to read transaction state for file " + f.getAbsolutePath(), e);
        }
    }

    private String[] getTransactionsInNeedOfRecovery() {
        List<String> unfinishedTxs = new ArrayList<String>();
        for(File f : getRecoveryStatePersistenceDirectory().listFiles(executeFilter)){
            try {
                if(getFileAgeInMs(f) > getMinAgeOfTransactionBeforeRelevantForRecovery()){ //TODO use transaction timeout?? or maybe we just list all EXECUTEs, and TX Manager is clever enough to know its in the middle of committing/rolling back some of them, ie the ones that are not in need of recovery??
                    String content = read(f);
                    unfinishedTxs.add(content);
                    log.log(Level.INFO, "Unfinished transaction found by generic resource adapter: " + f.getName());
                }
            } catch (NoSuchFileException e) {
                //the list of files has become out of date because
                //another XAResource has tidied away the transaction.
                //that means it no longer needs recovering :-)
                //summary: move along, nothing to see here.
            }
        }
        return unfinishedTxs.toArray(new String[0]);
    }

	private long getFileAgeInMs(File f) throws NoSuchFileException {
        try{
            BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
            return System.currentTimeMillis() - attr.creationTime().toMillis();
        }catch(NoSuchFileException e){
            throw e; //catch and throw, coz this one is interesting. other IOExceptions arent.
        }catch(IOException e){
            return Integer.MAX_VALUE; //otherwise theres a danger it will never be considered for recovery!
        }
    }

    @Override
    public boolean setTransactionTimeout(int timeout) throws XAException {
        log.log(Level.INFO, "SET TRANSACTION TIMEOUT " + timeout);
        this.timeout = timeout;
        //TODO this is called on startup -> provide the callback with this?
        return true;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        //called when getConnection is called in the code which uses this resource. xid comes from the TM.
        //remember the xid, since once started, we need to remember the result of EXECUTE
        //as it is important for the PREPARE phase.  

        String s = XidImpl.asString(xid);
        log.log(Level.INFO, "START " + flags + "/" + s);

        //note the xid, since its needed in the call to EXECUTE
        getUnderlyingConnection().setCurrentTxId(s);
    }
}
