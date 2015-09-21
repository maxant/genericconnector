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

import java.io.Serializable;
import java.util.Objects;

import javax.resource.Referenceable;

/**
 * The resource injected into say a Servlet or EJB.  Used to bind a resource into
 * the active XA transaction.
 */
public interface TransactionAssistanceFactory extends Serializable, Referenceable, BasicTransactionAssistanceFactory {

    /** The application must register a callback
     * which can be used to commit or rollback transactions
     * as well as recover unfinished transactions.
     * Normally this is called once during application startup.
     * That way, the callback is available as soon as recovery 
     * might need it, e.g. in the scenario where the server
     * is starting after a crash. */
    public void registerCommitRollbackRecovery(CommitRollbackRecoveryCallback commitRollbackRecoveryCallback);

    /** unregister the callback previously registered.
     * Note the resource adapter can only ever contain one 
     * callback per connection factory, and will fail hard
     * if you don't unregister before re-registering a callback.
     * Normally this is only called when an application is shutdown. */
    public void unregisterCommitRollbackRecovery();

    /** Classes with this interface are registered with the 
        TransactionAssistanceFactory. */
    public static interface CommitRollbackRecoveryCallback extends CommitRollbackCallback {
        
        /** The container will call this function during
         * recovery which should call the EIS and must return 
         * transaction IDs which are known to be incomplete (not 
         * yet committed or rolled back). Note that if the 
         * Resource Adapter is configured to manage transaction
         * state internally, then this method will not
         * be called and can have an empty implementation. */
        String[] getTransactionsInNeedOfRecovery();

        /** Builder enabling use of Java 8 SAMs */ 
        public static class Builder {
            private CommitRollbackFunction commit;
            private CommitRollbackFunction rollback;
            private RecoveryFunction recovery;
            public Builder withCommit(CommitRollbackFunction commit){
                this.commit = commit;
                return this;
            }
            public Builder withRollback(CommitRollbackFunction rollback){
                this.rollback = rollback;
                return this;
            }
            public Builder withRecovery(RecoveryFunction recovery){
                this.recovery = recovery;
                return this;
            }
            public CommitRollbackRecoveryCallback build(){
                Objects.requireNonNull(commit, "Please call withCommit(...)");
                Objects.requireNonNull(rollback, "Please call withRollback(...)");
                //recovery is optional, since you can configure adapter to handle state internally
                
                return new CommitRollbackRecoveryCallback(){
                    @Override
                    public void commit(String txid) throws Exception {
                        commit.apply(txid);
                    }
                    @Override
                    public void rollback(String txid) throws Exception {
                        rollback.apply(txid);
                    }
                    @Override
                    public String[] getTransactionsInNeedOfRecovery() {
                        if(recovery == null){
                            return new String[0];
                        }else{
                            return recovery.getTransactionsInNeedOfRecovery();
                        }
                    }
                };
            }
            
            public static interface RecoveryFunction {
                String[] getTransactionsInNeedOfRecovery();
            }
            
            public static interface CommitRollbackFunction {
                void apply(String txid) throws Exception;
            }
        }
    }
    
    
}
