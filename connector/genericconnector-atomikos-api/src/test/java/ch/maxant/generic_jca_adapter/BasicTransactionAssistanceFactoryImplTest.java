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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.resource.ResourceException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.atomikos.icatch.jta.UserTransactionManager;

public class BasicTransactionAssistanceFactoryImplTest {

	@Test
	public void testSuccessAll() throws ResourceException, IllegalStateException, RollbackException, SystemException {
		
		CommitRollbackCallback callback = new CommitRollbackCallback() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
			}
			@Override
			public void commit(String txid) throws Exception {
			}
		};
		final UserTransactionManager tm = mock(UserTransactionManager.class);
		when(tm.getStatus()).thenReturn(Status.STATUS_ACTIVE);
		Transaction tx = mock(Transaction.class);
		when(tm.getTransaction()).thenReturn(tx);

		//TEST
		TransactionConfigurator.setup("a", callback);
		
		BasicTransactionAssistanceFactory f = new BasicTransactionAssistanceFactoryImpl("a"){
			@Override
			protected UserTransactionManager getTM() {
				return tm;
			}
		};

		//TEST
		f.getTransactionAssistant();

		//ensure its enlisted
		ArgumentCaptor<XAResource> c = ArgumentCaptor.forClass(XAResource.class);
		verify(tx).enlistResource(c.capture());
		assertEquals(MicroserviceXAResource.class, c.getValue().getClass());
		assertEquals("a", ((MicroserviceXAResource)c.getValue()).getJndiName());
		
		TransactionConfigurator.unregisterMicroserviceResourceFactory("a");
		
		assertNull(TransactionConfigurator.getCommitRollbackCallback("a"));
	}

	@Test
	public void testNoTX() throws ResourceException, IllegalStateException, RollbackException, SystemException {
		final UserTransactionManager tm = mock(UserTransactionManager.class);
		when(tm.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);
		
		BasicTransactionAssistanceFactory f = new BasicTransactionAssistanceFactoryImpl("a"){
			@Override
			protected UserTransactionManager getTM() {
				return tm;
			}
		};

		try{
			//TEST
			f.getTransactionAssistant();
			fail("no exception");
		}catch(ResourceException e){
			//OK, expected
		}
	}
}
