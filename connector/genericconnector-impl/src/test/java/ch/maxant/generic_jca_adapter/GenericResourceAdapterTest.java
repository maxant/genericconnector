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

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.BeforeClass;
import org.junit.Test;

import ch.maxant.generic_jca_adapter.MiniContainer.Transaction;
import ch.maxant.generic_jca_adapter.MiniContainer.TransactionState;
import ch.maxant.generic_jca_adapter.TransactionAssistanceFactory.CommitRollbackRecoveryCallback;

public class GenericResourceAdapterTest {

	@BeforeClass
	public static void init(){
		for(File f : new File(System.getProperty("java.io.tmpdir")).listFiles()){
			if(f.getName().startsWith("exec") && f.getName().endsWith(".txt")){
				//perhaps its left over from a broken test - be nice to the person running this test :-)
				f.delete();
			}
		}
	}
	
	@Test
	public void testCannotRegisterTwice() {
		
		GenericResourceAdapter adapter = new GenericResourceAdapter();

		CommitRollbackRecoveryCallback commitRollbackRecoveryCallback = new CommitRollbackRecoveryCallback(){
			@Override
			public String[] getTransactionsInNeedOfRecovery() {
				return null;
			}

			@Override
			public void commit(String txid) throws Exception {
			}

			@Override
			public void rollback(String txid) throws Exception {
			}
		};
		adapter.registerCommitRollbackRecovery("managedConnectionFactoryId", commitRollbackRecoveryCallback);
		
		//fail, since a second registration isnt allowed
		try{
			adapter.registerCommitRollbackRecovery("managedConnectionFactoryId", commitRollbackRecoveryCallback);
			fail("no exception");
		}catch(IllegalStateException e){
			assertEquals("Unable to register commit/rollback/recovery for managed connection factory with ID 'managedConnectionFactoryId', because a callback has already been registered. Please unregister it first!", e.getMessage());
		}

		adapter.unregisterCommitRollbackRecovery("managedConnectionFactoryId");
		adapter.registerCommitRollbackRecovery("managedConnectionFactoryId", commitRollbackRecoveryCallback);
	}
	
	@Test
	public void testInContainer_WrongOrder() throws Exception {
		MiniContainer container = new MiniContainer();

		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();

		//method start - no transaction!!
	    
		//business code
		try{
			bookingFactory.getTransactionAssistant();
			fail("no exception");
		}catch(IllegalStateException e){
			assertEquals("please start a transaction before opening a connection", e.getMessage());
		}
	}

	@Test
	public void testInContainer_Commit() throws Exception {
		MiniContainer container = new MiniContainer();
		
		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();
		
		//setup commit/rollback caller
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		final AtomicInteger recoveryCalled = new AtomicInteger();
		
		registerCallbacks(bookingFactory, commitCalled, rollbackCalled, recoveryCalled, null, false);
		
		//method start
		final Transaction tx = container.startTransaction();
		
		//business code
		TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
		String bookingResponse = bookingTransactionAssistant.executeInActiveTransaction(new ExecuteCallback<String>() {
			@Override
			public String execute(String txid) throws Exception {
				return "1";
			}
		});
		bookingTransactionAssistant.close();
		
		//method exit
		container.finishTransaction();
		
		//assertions
		assertEquals(TransactionState.COMMITTING, tx.getTransactionState());
		assertEquals("1", bookingResponse);
		assertEquals(1, commitCalled.get());
		assertEquals(0, rollbackCalled.get());
		assertEquals(0, recoveryCalled.get());
		assertFalse(tx.isShouldRollback());
		assertEquals(0, tx.getExceptions().size());
	}
	
	@Test
	public void testInContainer_RollbackByWebservice() throws Exception {
		MiniContainer container = new MiniContainer();

		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();

		//setup commit/rollback caller
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		final AtomicInteger recoveryCalled = new AtomicInteger();

		registerCallbacks(bookingFactory, commitCalled, rollbackCalled, recoveryCalled, null, false);

		//method start
		final Transaction tx = container.startTransaction();
	    
		//business code
		try{
			TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
			bookingTransactionAssistant.executeInActiveTransaction(new ExecuteCallback<String>() {
				@Override
				public String execute(String txid) throws Exception {
					throw new Exception("1");
				}
			});
	        bookingTransactionAssistant.close();
			fail("no exception");
		}catch(Exception e){
			assertEquals("1", e.getMessage());
		}

        //method exit
        container.finishTransaction();
        
        //assertions
        assertEquals(TransactionState.ROLLINGBACK, tx.getTransactionState());
        assertEquals(0, commitCalled.get());
        assertEquals(1, rollbackCalled.get());
        assertEquals(0, recoveryCalled.get());
        assertFalse(tx.isShouldRollback());
        assertEquals(0, tx.getExceptions().size());
	}

	@Test
	public void testInContainer_RollbackByApplicationCode() throws Exception {
		MiniContainer container = new MiniContainer();

		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();

		//setup commit/rollback caller
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		final AtomicInteger recoveryCalled = new AtomicInteger();

		registerCallbacks(bookingFactory, commitCalled, rollbackCalled, recoveryCalled, null, false);

		//method start
		final Transaction tx = container.startTransaction();
	    
		//business code
		TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
		String bookingResponse = bookingTransactionAssistant.executeInActiveTransaction(new ExecuteCallback<String>() {
			@Override
			public String execute(String txid) throws Exception {
				return "1";
			}
		});
        bookingTransactionAssistant.close();

		container.setRollback(); //e.g. because business logic requires it, or some SQL cant be commit
		
        //method exit
        container.finishTransaction();
        
        //assertions
        assertEquals(TransactionState.ROLLINGBACK, tx.getTransactionState());
        assertEquals("1", bookingResponse);
        assertEquals(0, commitCalled.get());
        assertEquals(1, rollbackCalled.get());
        assertEquals(0, recoveryCalled.get());
        assertTrue(tx.isShouldRollback());
        assertEquals(0, tx.getExceptions().size());
	}
	
	@Test
	public void testInContainer_RecoveryCommit() throws Exception {
		MiniContainer container = new MiniContainer();

		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();

		//setup commit/rollback caller
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		final AtomicInteger recoveryCalled = new AtomicInteger();

		registerCallbacks(bookingFactory, commitCalled, rollbackCalled, recoveryCalled, null, false);

		//method start
		final Transaction tx = container.startTransaction();
	    
		//business code
		TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
		bookingTransactionAssistant.executeInActiveTransaction(new ExecuteCallback<String>() {
			@Override
			public String execute(String txid) throws Exception {
				return "1";
			}
		});
        bookingTransactionAssistant.close();

		//now pretend the container crashed and was restarted
		
        //start recovery
        List<String> txidsNeedingRecovery = container.recover();
       
        //assertions
        assertEquals(1, txidsNeedingRecovery.size());
		assertEquals(tx.getTxid(), txidsNeedingRecovery.get(0));
        assertEquals(TransactionState.COMMITTING, tx.getTransactionState());
        assertEquals(1, commitCalled.get());
        assertEquals(0, rollbackCalled.get());
        assertEquals(0, recoveryCalled.get());
        assertFalse(tx.isShouldRollback());
        assertEquals(0, tx.getExceptions().size());
	}
	
	@Test
	public void testInContainer_RecoveryRollbackBecauseOfWebservice() throws Exception {
		MiniContainer container = new MiniContainer();
		
		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();
		
		//setup commit/rollback caller
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		final AtomicInteger recoveryCalled = new AtomicInteger();

		registerCallbacks(bookingFactory, commitCalled, rollbackCalled, recoveryCalled, null, false);
		
		//method start
		final Transaction tx = container.startTransaction();
		
		//business code
		try{
			TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
			bookingTransactionAssistant.executeInActiveTransaction(new ExecuteCallback<String>() {
				@Override
				public String execute(String txid) throws Exception {
					throw new Exception("1");
				}
			});
	        bookingTransactionAssistant.close();
			fail("no exception");
		}catch(Exception e){
			assertEquals("1", e.getMessage());
		}
		
		//now pretend the container crashed and was restarted
		
		//start recovery
		List<String> txidsNeedingRecovery = container.recover();
		
		//assertions
		assertEquals(1, txidsNeedingRecovery.size());
		assertEquals(tx.getTxid(), txidsNeedingRecovery.get(0));
		assertEquals(TransactionState.ROLLINGBACK, tx.getTransactionState());
		assertEquals(0, commitCalled.get());
		assertEquals(1, rollbackCalled.get());
        assertEquals(0, recoveryCalled.get());
		assertFalse(tx.isShouldRollback());
        assertEquals(0, tx.getExceptions().size());
	}
	
	@Test
	public void testInContainer_RecoveryRollbackBecauseOfApplication() throws Exception {
		MiniContainer container = new MiniContainer();
		
		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();
		
		//setup commit/rollback caller
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		final AtomicInteger recoveryCalled = new AtomicInteger();

		registerCallbacks(bookingFactory, commitCalled, rollbackCalled, recoveryCalled, null, false);
		
		//method start
		final Transaction tx = container.startTransaction();
		
		//business code
		TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
		bookingTransactionAssistant.executeInActiveTransaction(new ExecuteCallback<String>() {
			@Override
			public String execute(String txid) throws Exception {
				return "1";
			}
		});
        bookingTransactionAssistant.close();

		container.setRollback();
		
		//now pretend the container crashed and was restarted
		
		//start recovery
		List<String> txidsNeedingRecovery = container.recover();
		
		//assertions
		assertEquals(1, txidsNeedingRecovery.size());
		assertEquals(tx.getTxid(), txidsNeedingRecovery.get(0));
		assertEquals(TransactionState.ROLLINGBACK, tx.getTransactionState());
		assertEquals(0, commitCalled.get());
		assertEquals(1, rollbackCalled.get());
		assertEquals(0, recoveryCalled.get());
		assertTrue(tx.isShouldRollback());
        assertEquals(0, tx.getExceptions().size());
	}
	
	@Test
	public void testInContainer_ExternalRecovery() throws Exception {
		MiniContainer container = new MiniContainer(false, "0");
		
		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();
		
		//setup commit/rollback caller
		final StringBuffer txid = new StringBuffer();
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		final AtomicInteger recoveryCalled = new AtomicInteger();

		registerCallbacks(bookingFactory, commitCalled, rollbackCalled, recoveryCalled, txid, false);
		
		//method start
		final Transaction tx = container.startTransaction();
		txid.append(tx.getTxid());
		
		//business code
		TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
		bookingTransactionAssistant.executeInActiveTransaction(new ExecuteCallback<String>() {
			@Override
			public String execute(String txid) throws Exception {
				return "1";
			}
		});
        bookingTransactionAssistant.close();

		container.setRollback();
		
		//now pretend the container crashed and was restarted
		
		//start recovery
		List<String> txidsNeedingRecovery = container.recover();
		
		//assertions
		assertEquals(1, txidsNeedingRecovery.size());
		assertEquals(tx.getTxid(), txidsNeedingRecovery.get(0));
		assertEquals(TransactionState.ROLLINGBACK, tx.getTransactionState());
		assertEquals(0, commitCalled.get());
		assertEquals(1, rollbackCalled.get());
		assertEquals(1, recoveryCalled.get());
		assertTrue(tx.isShouldRollback());
        assertEquals(0, tx.getExceptions().size());
	}
	
	@Test
	public void testInContainer_CommitFailureHandledLaterInRecovery() throws Exception {
		MiniContainer container = new MiniContainer();

		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();

		//setup commit/rollback caller
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		final AtomicInteger recoveryCalled = new AtomicInteger();

		registerCallbacks(bookingFactory, commitCalled, rollbackCalled, recoveryCalled, null, true);

		//method start
		final Transaction tx = container.startTransaction();
	    
		//business code
    	TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
        String bookingResponse = bookingTransactionAssistant.executeInActiveTransaction(new ExecuteCallback<String>() {
        	@Override
        	public String execute(String txid) throws Exception {
        		return "1";
        	}
		});
        bookingTransactionAssistant.close();

        //method exit
        container.finishTransaction();
        
		List<String> txidsNeedingRecovery = container.recover();
		
		//assertions
		assertEquals(1, txidsNeedingRecovery.size());
		assertEquals(tx.getTxid(), txidsNeedingRecovery.get(0));
        assertEquals(TransactionState.COMMITTING, tx.getTransactionState());
        assertEquals("1", bookingResponse);
        assertEquals(2, commitCalled.get());
        assertEquals(0, rollbackCalled.get());
        assertEquals(0, recoveryCalled.get());
        assertFalse(tx.isShouldRollback());
        assertEquals(1, tx.getExceptions().size()); //coz of commit exception
	}
	
	@Test
	public void testInContainer_RollbackFailureHandledLaterInRecovery() throws Exception {
		MiniContainer container = new MiniContainer();

		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();

		//setup commit/rollback caller
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		final AtomicInteger recoveryCalled = new AtomicInteger();

		registerCallbacks(bookingFactory, commitCalled, rollbackCalled, recoveryCalled, null, true);

		//method start
		final Transaction tx = container.startTransaction();
	    
		//business code
    	TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
        String bookingResponse = bookingTransactionAssistant.executeInActiveTransaction(new ExecuteCallback<String>() {
        	@Override
        	public String execute(String txid) throws Exception {
        		return "1";
        	}
		});
        bookingTransactionAssistant.close();

        container.setRollback();
        
        //method exit
        container.finishTransaction();
        
		List<String> txidsNeedingRecovery = container.recover();
		
		//assertions
		assertEquals(1, txidsNeedingRecovery.size());
		assertEquals(tx.getTxid(), txidsNeedingRecovery.get(0));
        assertEquals(TransactionState.ROLLINGBACK, tx.getTransactionState());
        assertEquals("1", bookingResponse);
        assertEquals(0, commitCalled.get());
        assertEquals(2, rollbackCalled.get());
        assertEquals(0, recoveryCalled.get());
        assertTrue(tx.isShouldRollback());
        assertEquals(1, tx.getExceptions().size()); //coz of commit exception
	}

	@Test
	public void testInContainer_Multithreadedness() throws Exception {
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		final AtomicInteger recoveryCalled = new AtomicInteger();
		final MiniContainer container = new MiniContainer();

		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();
		
		//register callbacks
		registerCallbacks(bookingFactory, commitCalled, rollbackCalled, recoveryCalled, null, false);

		final int numTasks = 500;
		final CountDownLatch cdl = new CountDownLatch(numTasks);
		final Random random = new Random();

		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		
		for(int i = 0; i < numTasks; i++){
			final int j = i;
			threadPool.execute(new Runnable(){
				@Override
				public void run() {
					
					try {
						Thread.sleep(random.nextInt(1));

						//injection
						TransactionAssistanceFactory bookingFactory = container.lookupAdapter();
						
						//method start
						final Transaction tx = container.startTransaction();
						
						//business code
						TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
						String bookingResponse = bookingTransactionAssistant.executeInActiveTransaction(new ExecuteCallback<String>() {
							@Override
							public String execute(String txid) throws Exception {
								return "1";
							}
						});
						bookingTransactionAssistant.close();
						
						if(j%2 == 0){
							container.setRollback();
						}
						
						//method exit
						container.finishTransaction();
						
						assertEquals(j%2==0 ? TransactionState.ROLLINGBACK : TransactionState.COMMITTING, tx.getTransactionState());
						assertEquals("1", bookingResponse);
						assertEquals(j%2==0, tx.isShouldRollback());
						assertEquals(0, tx.getExceptions().size());
					} catch (Exception e) {
						e.printStackTrace();
						fail("not expected");
					} finally {
						cdl.countDown();
					}
					
				}
			});
		}
		
		cdl.await();
		
		//assertions
        assertEquals(numTasks/2, commitCalled.get());
        assertEquals(numTasks/2, rollbackCalled.get());
        assertEquals(0, recoveryCalled.get());
	}

	private void registerCallbacks(TransactionAssistanceFactory bookingFactory,
			final AtomicInteger commitCalled,
			final AtomicInteger rollbackCalled,
			final AtomicInteger recoveryCalled, 
			final StringBuffer txid,
			final boolean throwExceptionOnCommitOrRollback) {
		bookingFactory.registerCommitRollbackRecovery(new CommitRollbackRecoveryCallback(){
			@Override
			public void commit(String txid) throws Exception {
				commitCalled.incrementAndGet();
				if(throwExceptionOnCommitOrRollback && commitCalled.get() == 1){
					throw new Exception("test failure");
				}
			}

			@Override
			public void rollback(String txid) throws Exception {
				rollbackCalled.incrementAndGet();
				if(throwExceptionOnCommitOrRollback && rollbackCalled.get() == 1){
					throw new Exception("test failure");
				}
			}

			@Override
			public String[] getTransactionsInNeedOfRecovery() {
				recoveryCalled.incrementAndGet();
				return txid == null ? null : new String[]{txid.toString()};
			}
		});
	}
	
}
