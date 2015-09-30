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

import javax.resource.ResourceException;
import javax.transaction.Status;
import javax.transaction.Transaction;

import com.atomikos.icatch.jta.UserTransactionManager;

/** Use this class to build a {@link TransactionAssistant}. Analagous to the factory
 * normally fetched from JNDI, but Atomikos doesn't appear to ship with an out-of-the-box
 * JNDI context like Bitronix does. For that reason, just instantiate one of these, in
 * order to create new TransactionAssistants. */
public class BasicTransactionAssistanceFactoryImpl implements BasicTransactionAssistanceFactory {

	private String jndiName;

	/**
	 * @param jndiName the name of the resource that this factory represents, eg the name of the microservice which this factory is in charge of
	 * committing, rolling back and recovering. 
	 */
	public BasicTransactionAssistanceFactoryImpl(String jndiName) {
		this.jndiName = jndiName;
	}

	/** before calling this method, please ensure you have called {@link TransactionConfigurator#setup(String, CommitRollbackCallback)} */
	@Override
	public TransactionAssistant getTransactionAssistant() throws ResourceException {
		//enlist a new resource into the transaction. it will be delisted, when its closed.
		final CommitRollbackCallback commitRollbackCallback = TransactionConfigurator.getCommitRollbackCallback(jndiName);
		MicroserviceXAResource ms = new MicroserviceXAResource(jndiName, commitRollbackCallback);
		UserTransactionManager utm = getTM();
		try {
			if(utm.getStatus() == Status.STATUS_NO_TRANSACTION){
				throw new ResourceException("no transaction found. please start one before getting the transaction assistant. status was: " + utm.getStatus());
			}
			Transaction tx = utm.getTransaction();
			tx.enlistResource(ms);
			return new AtomikosTransactionAssistantImpl(ms);
		} catch (Exception e) {
			throw new ResourceException("Unable to get transaction status", e);
		}
	}

	protected UserTransactionManager getTM() {
		return new UserTransactionManager();
	}
	
}
