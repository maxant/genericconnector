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

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.atomikos.icatch.jta.UserTransactionManager;

class AtomikosTransactionAssistantImpl implements TransactionAssistant {

	private MicroserviceXAResource ms;

	AtomikosTransactionAssistantImpl(MicroserviceXAResource ms) {
		this.ms = ms;
	}

	@Override
	public <O> O executeInActiveTransaction(ExecuteCallback<O> c) throws Exception {
		return ms.executeInActiveTransaction(c);
	}

	@Override
	public void close() {
		UserTransactionManager utm = getTransactionManager();
		try {
			if(utm.getStatus() == Status.STATUS_NO_TRANSACTION){
				throw new RuntimeException("no transaction found. please start one before getting the transaction assistant. status was: " + utm.getStatus());
			}
			Transaction tx = utm.getTransaction();
			tx.delistResource(ms, ms.getUnderlyingConnection().wasExecuteSuccessful() ? XAResource.TMSUCCESS : XAResource.TMFAIL);
		} catch (Exception e) {
			throw new RuntimeException("Unable to delist resource from transaction", e);
		}
	}

	protected UserTransactionManager getTransactionManager() {
		return new UserTransactionManager();
	}

}
