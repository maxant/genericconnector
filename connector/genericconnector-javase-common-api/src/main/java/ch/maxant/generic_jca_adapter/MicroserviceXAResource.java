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

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

/** the XA resource used by Java SE */
public class MicroserviceXAResource extends AbstractTransactionAssistanceXAResource {

	private static final long serialVersionUID = 1L;

	private final UnderlyingConnectionImpl underlyingConnection;

	/** how old do transactions have to be before we consider cleaning them up? 
	 * since recovery runs async, we need to be careful not to start recovering something thats still active!
	 * TODO could we ask BTM for the active transactions, and skip those ones? */
	private static long minAgeOfTransactionInMSBeforeRelevantForRecovery = 30000L;

	/** where should we store our information about transaction state? */
	private static File recoveryStatePersistenceDirectory = new File(".");

	private String jndiName;

	MicroserviceXAResource(String jndiName, final CommitRollbackCallback commitRollbackCallback) {
		this.jndiName = jndiName;
		this.underlyingConnection = new UnderlyingConnectionImpl() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
				commitRollbackCallback.rollback(txid);
			}
			@Override
			public void commit(String txid) throws Exception {
				commitRollbackCallback.commit(txid);
			}
		};
	}

	@Override
	protected long getMinAgeOfTransactionBeforeRelevantForRecovery() {
		return minAgeOfTransactionInMSBeforeRelevantForRecovery;
	}
	
	@Override
	protected File getRecoveryStatePersistenceDirectory() {
		return recoveryStatePersistenceDirectory;
	}
	
	@Override
	protected UnderlyingConnection getUnderlyingConnection() {
		return underlyingConnection;
	}

	@Override
	public boolean isSameRM(XAResource xares) throws XAException {
		log.log(Level.FINEST, "isSameRM " + xares);
		
        if(xares instanceof MicroserviceXAResource){
        	MicroserviceXAResource other = (MicroserviceXAResource) xares;
            if(this.jndiName.equals(other.jndiName)){
                return true;
            }
        }
        return false;
		// TODO hmm not sure about this
	}

	/** execute the given callback in the active transaction */
	<O> O executeInActiveTransaction(ExecuteCallback<O> c) throws Exception {
		return underlyingConnection.execute(c, recoveryStatePersistenceDirectory);
	}

	/** setup the location of where recovery state is stored, and how old it must be before recovery is attempted. defaults are the working directory and 30 seconds. */
	public static void configure(long minAgeOfTransactionInMSBeforeRelevantForRecovery, File recoveryStatePersistenceDirectory){
		MicroserviceXAResource.minAgeOfTransactionInMSBeforeRelevantForRecovery = minAgeOfTransactionInMSBeforeRelevantForRecovery;
		MicroserviceXAResource.recoveryStatePersistenceDirectory = recoveryStatePersistenceDirectory;
	}

	String getJndiName() {
		return jndiName;
	}
}
