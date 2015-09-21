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
import java.util.logging.Level;

import javax.resource.ResourceException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

/**
 * {@link XAResource} used by the transaction assistance JCA Adapter to bind callbacks 
 * into transactions.
 */
public class TransactionAssistanceXAResource extends AbstractTransactionAssistanceXAResource {

	private static final long serialVersionUID = 1L;

	/** the resource related to this {@link XAResource} */
    private ManagedTransactionAssistance conn;

    public TransactionAssistanceXAResource(ManagedTransactionAssistance conn) {
        this.conn = conn;
    }

    @Override
    protected long getMinAgeOfTransactionBeforeRelevantForRecovery() {
    	return conn.getMinAgeOfTransactionBeforeRelevantForRecovery();
    }
    
    @Override
    protected File getRecoveryStatePersistenceDirectory() {
    	return conn.getRecoveryStatePersistenceDirectory();
    }

    @Override
    protected boolean isHandleRecoveryInternally() {
    	return conn.isHandleRecoveryInternally();
    }
    
    @Override
    protected UnderlyingConnection getUnderlyingConnection() {
    	return new UnderlyingConnection() {
			
			@Override
			public void rollback(String txid) throws Exception {
				conn.getCommitRollbackRecoveryCallback().rollback(txid);
			}
			
			@Override
			public void commit(String txid) throws Exception {
				conn.getCommitRollbackRecoveryCallback().commit(txid);
			}
			
			@Override
			public boolean wasExecuteSuccessful() {
				return conn.wasExecuteSuccessful();
			}
			
			@Override
			public void setCurrentTxId(String txId) {
				conn.setCurrentTxId(txId);
			}
			
			@Override
			public String[] getTransactionsInNeedOfRecovery() {
				return conn.getCommitRollbackRecoveryCallback().getTransactionsInNeedOfRecovery();
			}
			
			@Override
			public void cleanup() throws ResourceException {
				conn.cleanup();
			}
		};
    }
    
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

}
