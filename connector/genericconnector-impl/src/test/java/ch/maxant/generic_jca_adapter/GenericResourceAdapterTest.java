package ch.maxant.generic_jca_adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import ch.maxant.generic_jca_adapter.MiniContainer.Transaction;
import ch.maxant.generic_jca_adapter.MiniContainer.TransactionState;
import ch.maxant.generic_jca_adapter.TransactionAssistanceFactory.CommitRollbackRecoveryCallback;

public class GenericResourceAdapterTest {

	@Test(expected=IllegalStateException.class)
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
		adapter.registerCommitRollbackRecovery("managedConnectionFactoryId", commitRollbackRecoveryCallback);
	}
	
	@Test
	public void testInContainer_Commit() throws Exception {
		MiniContainer container = new MiniContainer();

		//injection
		TransactionAssistanceFactory bookingFactory = container.lookupAdapter();

		//setup commit/rollback caller
		final AtomicInteger commitCalled = new AtomicInteger();
		final AtomicInteger rollbackCalled = new AtomicInteger();
		bookingFactory.registerCommitRollbackRecovery(new CommitRollbackRecoveryCallback(){
			@Override
			public void commit(String txid) throws Exception {
				commitCalled.incrementAndGet();
			}
			
			@Override
			public void rollback(String txid) throws Exception {
				rollbackCalled.incrementAndGet();
			}

			@Override
			public String[] getTransactionsInNeedOfRecovery() {
				return null;
			}
		});

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

        //method exit
        container.commit(tx);
        
       
        //assertions
        assertEquals(TransactionState.COMMITTING, container.getTransactionState(tx));
        assertEquals("1", bookingResponse);
        assertEquals(1, commitCalled.get());
        assertEquals(0, rollbackCalled.get());
	}

	@Test
	public void testInContainer_RollbackByWebservice() throws Exception {
        fail("otherAsserts");
	}

	@Test
	public void testInContainer_RollbackByApplicationCode() throws Exception {
        fail("otherAsserts");
	}
	
	@Test
	public void testInContainer_Recovery() throws Exception {
        fail("otherAsserts");
	}
	
}
