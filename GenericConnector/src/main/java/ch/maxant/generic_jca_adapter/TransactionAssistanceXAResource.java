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

import ch.maxant.generic_jca_adapter.TransactionAssistanceFactory.CommitRollbackRecoveryCallback;

/* {@link XAResource} constants:
 * 
 * TMENDRSCAN Ends a recovery scan
 * TMFAIL Disassociates the caller and marks the transaction branch rollback-only.
 * TMJOIN Caller is joining existing transaction branch.
 * TMNOFLAGS Use TMNOFLAGS to indicate no flags value is selected.
 * TMONEPHASE Caller is using one-phase optimization.
 * TMRESUME Caller is resuming association with a suspended transaction branch.
 * TMSTARTRSCAN Starts a recovery scan.
 * TMSUCCESS Disassociates caller from a transaction branch.
 * TMSUSPEND Caller is suspending (not ending) its association with a transaction branch.
 * XA_OK The transaction work has been prepared normally.
 * XA_RDONLY The transaction branch has been read-only and has been committed.
 * 
 * {@link XAException} constants:
 * 
 * XA_HEURCOM The transaction branch has been heuristically committed.
 * XA_HEURHAZ The transaction branch may have been heuristically completed.
 * XA_HEURMIX The transaction branch has been heuristically committed and rolled back.
 * XA_HEURRB The transaction branch has been heuristically rolled back.
 * XA_NOMIGRATE Resumption must occur where the suspension occurred.
 * XA_RBBASE The inclusive lower bound of the rollback codes.
 * XA_RBCOMMFAIL Indicates that the rollback was caused by a communication failure.
 * XA_RBDEADLOCK A deadlock was detected.
 * XA_RBEND The inclusive upper bound of the rollback error code.
 * XA_RBINTEGRITY A condition that violates the integrity of the resource was detected.
 * XA_RBOTHER The resource manager rolled back the transaction branch for a reason not on this list.
 * XA_RBPROTO A protocol error occurred in the resource manager.
 * XA_RBROLLBACK Indicates that the rollback was caused by an unspecified reason.
 * XA_RBTIMEOUT A transaction branch took too long.
 * XA_RBTRANSIENT May retry the transaction branch.
 * XA_RDONLY The transaction branch was read-only and has been committed.
 * XA_RETRY Routine returned with no effect and may be reissued.
 * XAER_ASYNC There is an asynchronous operation already outstanding.
 * XAER_DUPID The XID already exists.
 * XAER_INVAL Invalid arguments were given.
 * XAER_NOTA The XID is not valid.
 * XAER_OUTSIDE The resource manager is doing work outside a global transaction.
 * XAER_PROTO Routine was invoked in an improper context.
 * XAER_RMERR A resource manager error has occurred in the transaction branch.
 * XAER_RMFAIL Resource manager is unavailable.
 */

/**
 * {@link XAResource} used by the transaction assistance JCA Adapter to bind callbacks 
 * into transactions.
 */
public class TransactionAssistanceXAResource implements XAResource, Serializable {

	private static final long serialVersionUID = 1L;
	
	private static FileFilter executeFilter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.getName().toLowerCase().startsWith("exec.");
		}
	};

	private final Logger log = Logger.getLogger(this.getClass().getName());

    /** the resource related to this {@link XAResource} */
    private ManagedTransactionAssistance conn;

    /** default timeout, as well as that set by the system */
    private int timeout = 300;

    public TransactionAssistanceXAResource(ManagedTransactionAssistance conn) {
        this.conn = conn;
    }

    /**
     * Commits the global transaction specified by xid.
     * @param xid - A global transaction identifier
     * @param onePhase - If true, the resource manager should use a one-phase commit protocol to commit the work done on behalf of xid.
     * @throws XAException - An error has occurred. Possible XAExceptions are XA_HEURHAZ, XA_HEURCOM, XA_HEURRB, XA_HEURMIX, XAER_RMERR, 
     * XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     * 
     * If the resource manager did not commit the transaction and the parameter onePhase is set to true, the resource manager 
     * may throw one of the XA_RB* exceptions. Upon return, the resource manager has rolled back the 
     * branch's work and has released all held resources.
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        String s = XidImpl.asString(xid);
		log.log(Level.INFO, "COMMIT " + onePhase + "/" + s);

        //regardless of onePhase, we need to tell the external system that we are done and 
        //the previous call to the EXECUTE method should be COMMITTED.
        try{
            conn.getCommitRollbackRecoveryCallback().commit(s);

            if(conn.isHandleRecoveryInternally()){
            	cleanupInternalTransactionState(xid);
            }
            
            conn.cleanup();

        }catch(Exception e){
            log.log(Level.SEVERE, "Failed to COMMIT", e);
            int var = XAException.XA_RETRY;
			throw new XAException(var);
        }
    }

    /**
     * Ends the work performed on behalf of a transaction branch. The resource manager disassociates 
     * the XA resource from the transaction branch specified and lets the transaction complete.
     * 
     * If TMSUSPEND is specified in the flags, the transaction branch is temporarily suspended 
     * in an incomplete state. The transaction context is in a suspended state and must be 
     * resumed via the start method with TMRESUME specified.
     * 
     * If TMFAIL is specified, the portion of work has failed. The resource manager 
     * may mark the transaction as rollback-only
     * 
     * If TMSUCCESS is specified, the portion of work has completed successfully.
     * 
     * @param xid - A global transaction identifier that is the same as the identifier 
     * used previously in the start method.
     * 
     * @param flags - One of TMSUCCESS, TMFAIL, or TMSUSPEND.
     * 
     * @throws XAException - An error has occurred. Possible XAException values are XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, XAER_PROTO, or XA_RB*.
     */
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

    /**
     * Tells the resource manager to forget about a heuristically completed transaction branch.
     * 
     * @param xid - A global transaction identifier.
     * @throws XAException - An error has occurred. Possible exception values are XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     */
    @Override
    public void forget(Xid xid) throws XAException {
        log.log(Level.INFO, "FORGET " + XidImpl.asString(xid));

        if(conn.isHandleRecoveryInternally()){
        	cleanupInternalTransactionState(xid);
        }else{
        	//TODO send this to the callback?
        }
    }

    /**
     * Obtains the current transaction timeout value set for this XAResource instance. 
     * If XAResource.setTransactionTimeout was not used prior to invoking this method, 
     * the return value is the default timeout set for the resource manager; otherwise, 
     * the value used in the previous setTransactionTimeout call is returned.
     * 
     * @return the transaction timeout value in seconds.
     * 
     * @throws XAException - An error has occurred. Possible exception values are XAER_RMERR and XAER_RMFAIL.
     */
    @Override
    public int getTransactionTimeout() throws XAException {
        // TODO from config?
        return timeout;
    }

    /**
     * This method is called to determine if the resource manager instance represented by 
     * the target object is the same as the resouce manager instance represented by 
     * the parameter <code>xares</code>.
     * 
     * @param xares - An XAResource object whose resource manager instance is to be 
     * compared with the resource manager instance of the target object.
     * 
     * @return true if it's the same RM instance; otherwise false.
     * 
     * @throws XAException - An error has occurred. Possible exception values are XAER_RMERR and XAER_RMFAIL.
     */
    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        log.log(Level.FINEST, "isSameRM " + xares);

        //TODO hmm not sure about this

        if(xares instanceof TransactionAssistanceXAResource){
        	TransactionAssistanceXAResource other = (TransactionAssistanceXAResource) xares;
        	if(this.conn.getManagedConnectionFactoryId().equals(other.conn.getManagedConnectionFactoryId())){
        		return true;
        	}
        }
        return false;
    }

    /**
     * Ask the resource manager to prepare for a transaction commit of the transaction specified in xid.
     * 
     * @param xid - A global transaction identifier.
     * 
     * @return A value indicating the resource manager's vote on the outcome of the transaction. 
     * The possible values are: XA_RDONLY or XA_OK. If the resource manager wants to roll back 
     * the transaction, it should do so by raising an appropriate XAException in the prepare method.
     * 
     * @throws XAException - An error has occurred. Possible exception values are: XA_RB*, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     */
    @Override
    public int prepare(Xid xid) throws XAException {
        log.log(Level.INFO, "PREPARE " + XidImpl.asString(xid));

        //if the EXECUTE was unsuccessful, we must not let a COMMIT occur, since it will fail
        //and COMMITs may not fail for business reasons, only technical reasons.

        //TODO 7.6.2.8 of the JCA spec 1.6 says we must not erase knowledge 
        //of the transaction branch until commit or rollback is called.
        //if we aren't persistent, what will happen?  is it up to us
        //to tell the TM that something is still outstanding, or does 
        //it tell us?  The point is that the TM may call the recover
        //method to find out which transactions are still open.
        
        if(!conn.wasExecuteSuccessful()){
        	//vote to rollback :o(
            throw new XAException(XAException.XA_RBROLLBACK);
        }else{
        	//a successful execute is a guarantee that we can commit => therefore vote "OK to commit"
        	return XAResource.XA_OK;
        }
    }

    /**
     * Obtains a list of prepared transaction branches from a resource manager. The transaction manager calls 
     * this method during recovery to obtain the list of transaction branches that are currently in prepared 
     * or heuristically completed states.
     * 
     * @param flag - One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS must be used when no other flags are set in the parameter.
     * 
     * @return The resource manager returns zero or more XIDs of the transaction branches that are 
     * currently in a prepared or heuristically completed state. If an error occurs during the operation, 
     * the resource manager should throw the appropriate XAException.
     * 
     * @throws XAException - An error has occurred. Possible values are XAER_RMERR, XAER_RMFAIL, XAER_INVAL, and XAER_PROTO.
     */
    @Override
    public Xid[] recover(int flag) throws XAException {
        //TODO when is this called? 20150625 - i think after a restart when things go really wrong.

    	List<Xid> xids = new ArrayList<>();
    	
    	switch(flag) {
    	case (XAResource.TMSTARTRSCAN):
    		log.log(Level.INFO, "RECOVER TMSTARTRSCAN");
    		CommitRollbackRecoveryCallback callback = conn.getCommitRollbackRecoveryCallback();
			if(callback != null){
				final String[] unfinishedTxIds;
				if(conn.isHandleRecoveryInternally()){
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

	/**
     * Informs the resource manager to roll back work done on behalf of a transaction branch.
     * 
     * @param xid - A global transaction identifier.
     * 
     * @throws XAException - An error has occurred. Possible XAExceptions are XA_HEURHAZ, XA_HEURCOM, 
     * XA_HEURRB, XA_HEURMIX, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     * 
     * If the transaction branch is already marked rollback-only the resource manager may 
     * throw one of the XA_RB* exceptions. Upon return, the resource manager has rolled 
     * back the branch's work and has released all held resources.
     */
    @Override
    public void rollback(Xid xid) throws XAException {
        String s = XidImpl.asString(xid);
		log.log(Level.INFO, "ROLLBACK " + s);
        
        try{
            conn.getCommitRollbackRecoveryCallback().rollback(s);
            
            if(conn.isHandleRecoveryInternally()){
            	cleanupInternalTransactionState(xid);
            }

            conn.cleanup();
        }catch(Exception e){
        	log.log(Level.WARNING, "failed to rollback for txid " + s, e);
            throw new XAException(XAException.XA_RETRY);
        }
    }

    private void cleanupInternalTransactionState(Xid xid) {
    	boolean found = false;
		for(File f : conn.getRecoveryStatePersistenceDirectory().listFiles(executeFilter)){
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
    	List<String> unfinishedTxs = new ArrayList<>();
		for(File f : conn.getRecoveryStatePersistenceDirectory().listFiles(executeFilter)){
			try {
				if(getFileAgeInMs(f) > 30000){ //TODO use transaction timeout?? or maybe we just list all EXECUTEs, and TX Manager is clever enough to know its in the middle of committing/rolling back some of them, ie the ones that are not in need of recovery??
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
			throw e;
		}catch(IOException e){
			return Integer.MAX_VALUE; //otherwise theres a danger it will never be considered for recovery!
		}
	}

	/**
     * Sets the current transaction timeout value for this XAResource instance. Once set, this 
     * timeout value is effective until setTransactionTimeout is invoked again with a different 
     * value. To reset the timeout value to the default value used by the resource manager, set 
     * the value to zero. If the timeout operation is performed successfully, the method returns 
     * true; otherwise false. If a resource manager does not support explicitly setting the transaction 
     * timeout value, this method returns false.
     * 
     * @param seconds - The transaction timeout value in seconds.
     * 
     * @return true if the transaction timeout value is set successfully; otherwise false.
     * 
     * @throws XAException - An error has occurred. Possible exception values are XAER_RMERR, XAER_RMFAIL, or XAER_INVAL.
     */
    @Override
    public boolean setTransactionTimeout(int timeout) throws XAException {
        log.log(Level.INFO, "SET TRANSACTION TIMEOUT " + timeout);
        this.timeout = timeout;
        //TODO this is called on startup -> provide the callback with this?
        return true;
    }

    /**
     * Starts work on behalf of a transaction branch specified in xid. If TMJOIN is specified, 
     * the start applies to joining a transaction previously seen by the resource manager. If 
     * TMRESUME is specified, the start applies to resuming a suspended transaction specified 
     * in the parameter xid. If neither TMJOIN nor TMRESUME is specified and the transaction 
     * specified by xid has previously been seen by the resource manager, the resource manager 
     * throws the XAException exception with XAER_DUPID error code.
     * 
     * @param xid - A global transaction identifier to be associated with the resource.
     * 
     * @param flags - One of TMNOFLAGS, TMJOIN, or TMRESUME.
     * 
     * @throws XAException - An error has occurred. Possible exceptions are XA_RB*, 
     * XAER_RMERR, XAER_RMFAIL, XAER_DUPID, XAER_OUTSIDE, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     */
    @Override
    public void start(Xid xid, int flags) throws XAException {
        //called when getConnection is called in the code which uses this resource. xid comes from the TM.
        //remember the xid, since once started, we need to remember the result of EXECUTE
        //as it is important for the PREPARE phase.  we want #isSameRM to return true only if
        //the same RM is passed i.e. the one for the given TX.

        String s = XidImpl.asString(xid);
		log.log(Level.INFO, "START " + flags + "/" + s);

        //note the xid, since its needed in the call to EXECUTE
        conn.setCurrentTxId(s);
    }
}
