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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.resource.ResourceException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.Test;

public class MicroserviceXAResourceTest {

	@Test
	public void testIsSameRM() throws XAException {
		MicroserviceXAResource xa = new MicroserviceXAResource("a", null);
		MicroserviceXAResource xb = new MicroserviceXAResource("a", null);
		
		assertTrue(xa.isSameRM(xb));
		assertTrue(xb.isSameRM(xa));

		MicroserviceXAResource xc = new MicroserviceXAResource("c", null);

		assertFalse(xa.isSameRM(xc));
		assertFalse(xb.isSameRM(xc));
		assertFalse(xc.isSameRM(xa));
		assertFalse(xc.isSameRM(xb));
		
		XAResource xd = mock(XAResource.class);
		assertFalse(xa.isSameRM(xd));
	}
	
	@Test
	public void testExecuteSuccess() throws Exception {
		MicroserviceXAResource xa = new MicroserviceXAResource("a", new CommitRollbackCallback() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
				fail("not expected here");
			}
			@Override
			public void commit(String txid) throws Exception {
				fail("not expected here");
			}
		});
		
		xa.getUnderlyingConnection().setCurrentTxId("tx1");
		final AtomicInteger i = new AtomicInteger(1);
		String result = xa.executeInActiveTransaction(new ExecuteCallback<String>() {
			@Override
			public String execute(String txid) throws Exception {
				i.incrementAndGet();
				assertEquals("tx1", txid);
				return "asdf";
			}
		});
		
		assertEquals("asdf", result);
		
		assertEquals(2, i.get());
		assertTrue(xa.getUnderlyingConnection().wasExecuteSuccessful());
	}

	@Test
	public void testExecuteFail() throws Exception {
		MicroserviceXAResource xa = new MicroserviceXAResource("a", new CommitRollbackCallback() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
				fail("not expected here");
			}
			@Override
			public void commit(String txid) throws Exception {
				fail("not expected here");
			}
		});
		
		xa.getUnderlyingConnection().setCurrentTxId("tx1");
		final AtomicInteger i = new AtomicInteger(1);
		try{
			xa.executeInActiveTransaction(new ExecuteCallback<String>() {
				@Override
				public String execute(String txid) throws Exception {
					i.incrementAndGet();
					assertEquals("tx1", txid);
					throw new Exception("simulated");
				}
			});
			fail("no exception");
		}catch(Exception e){
			assertEquals("simulated", e.getMessage());
		}
		
		assertEquals(2, i.get());
		assertFalse(xa.getUnderlyingConnection().wasExecuteSuccessful());
	}

	@Test
	public void testHowRecoveryIsDone() {
		MicroserviceXAResource xa = new MicroserviceXAResource("a", null);
		
		assertTrue(xa.isHandleRecoveryInternally());

		try{
			xa.getUnderlyingConnection().getTransactionsInNeedOfRecovery();
			fail("no exception");
		}catch(UnsupportedOperationException e){
			//OK
		}
	}

	@Test
	public void testCleanup() throws ResourceException {
		MicroserviceXAResource xa = new MicroserviceXAResource("a", null);
		
		xa.getUnderlyingConnection().setCurrentTxId("a");
		xa.getUnderlyingConnection().cleanup();
		try{
			xa.getUnderlyingConnection().wasExecuteSuccessful();
			fail("no exception");
		}catch(NullPointerException e){
			//OK
		}
	}

	@Test
	public void testCommit() throws XAException {
		final AtomicInteger i = new AtomicInteger(1);
		MicroserviceXAResource xa = new MicroserviceXAResource("a", new CommitRollbackCallback() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
				fail("not expected here");
			}
			@Override
			public void commit(String txid) throws Exception {
				i.incrementAndGet();
			}
		});
		xa.commit(new Xid() {
			@Override
			public byte[] getGlobalTransactionId() {
				return "gtxid".getBytes();
			}
			
			@Override
			public int getFormatId() {
				return 99;
			}
			
			@Override
			public byte[] getBranchQualifier() {
				return "bq".getBytes();
			}
		}, false);
		
		assertEquals(2, i.get());
	}
	
	@Test
	public void testRollback() throws XAException {
		final AtomicInteger i = new AtomicInteger(1);
		MicroserviceXAResource xa = new MicroserviceXAResource("a", new CommitRollbackCallback() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
				i.incrementAndGet();
			}
			@Override
			public void commit(String txid) throws Exception {
				fail("not expected here");
			}
		});
		xa.rollback(new Xid() {
			@Override
			public byte[] getGlobalTransactionId() {
				return "gtxid".getBytes();
			}
			
			@Override
			public int getFormatId() {
				return 99;
			}
			
			@Override
			public byte[] getBranchQualifier() {
				return "bq".getBytes();
			}
		});
		
		assertEquals(2, i.get());
	}

	@Test
	public void testGetJndiName() throws XAException {
		MicroserviceXAResource xa = new MicroserviceXAResource("a", null);
		assertEquals("a", xa.getJndiName());
	}
	
	@Test
	public void testConfigure() throws XAException, IOException {
		File f = File.createTempFile("prefix", "suffix");
		f.deleteOnExit();

		MicroserviceXAResource.configure(2, f);
		
		MicroserviceXAResource xa = new MicroserviceXAResource("a", null);
		assertEquals(2L, xa.getMinAgeOfTransactionBeforeRelevantForRecovery());
		assertTrue(xa.getRecoveryStatePersistenceDirectory().getName(), xa.getRecoveryStatePersistenceDirectory().getName().startsWith("prefix"));
		assertTrue(xa.getRecoveryStatePersistenceDirectory().getName(), xa.getRecoveryStatePersistenceDirectory().getName().endsWith("suffix"));
		
		MicroserviceXAResource.configure(30000, new File(".")); //otherwise other tests fail - they need a folder!
	}

}
