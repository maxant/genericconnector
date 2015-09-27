package ch.maxant.generic_jca_adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.Test;

import com.atomikos.icatch.jta.UserTransactionManager;

public class AtomikosTransactionAssistantImplTest {

	@Test
	public void testCloseDelistsResource_Success() throws Exception {
		testCloseDelistsResource(XAResource.TMSUCCESS);
	}

	@Test
	public void testCloseDelistsResource_Fail() throws Exception {
		testCloseDelistsResource(XAResource.TMFAIL);
	}
	
	private void testCloseDelistsResource(final int result) throws Exception {
		final UserTransactionManager tm = mock(UserTransactionManager.class);
		final Transaction tx = mock(Transaction.class);
		when(tm.getTransaction()).thenReturn(tx);
		MicroserviceXAResource ms = getMs();
		final AtomicInteger count = new AtomicInteger();
		ms.start(getXid(), 0);
		AtomikosTransactionAssistantImpl impl = new AtomikosTransactionAssistantImpl(ms){
			@Override
			protected UserTransactionManager getTransactionManager() {
				return tm;
			}
		};

		try{
			//TEST
			impl.executeInActiveTransaction(new ExecuteCallback<Void>() {
				@Override
				public Void execute(String txid) throws Exception {
					count.incrementAndGet();
					if(result == XAResource.TMSUCCESS){
						return null; //no exception => TMSUCCESS
					}else{
						throw new Exception(); // => TMFAIL
					}
				}
			});
			if(result == XAResource.TMFAIL) fail("no exception");
		}catch(Exception e){
			if(result == XAResource.TMSUCCESS) fail("exception not expected");
		}

		//TEST
		impl.close();

		assertEquals(1, count.get());
		verify(tx, times(1)).delistResource(eq(ms), eq(result));
	}

	private MicroserviceXAResource getMs() {
		MicroserviceXAResource ms = new MicroserviceXAResource("a", new CommitRollbackCallback() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
			}
			@Override
			public void commit(String txid) throws Exception {
			}
		});
		return ms;
	}

	private Xid getXid() {
		return new Xid() {
			@Override
			public byte[] getGlobalTransactionId() {
				return "gtxid".getBytes();
			}
			@Override
			public int getFormatId() {
				return 1;
			}
			@Override
			public byte[] getBranchQualifier() {
				return "bq".getBytes();
			}
		};
	}
	
}
