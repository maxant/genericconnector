package ch.maxant.generic_jca_adapter;

import java.sql.Connection;
import java.sql.ResultSet;

import javax.sql.XAConnection;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.xa.XATransactionalResource;
import com.atomikos.datasource.xa.jdbc.JdbcTransactionalResource;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;

public class TestAtomikos {

	public static void main(String[] args) throws Exception {

        MicroserviceResource ms = new MicroserviceResource(new CommitRollbackHandler() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
				System.out.println(); //TODO
			}
			@Override
			public void commit(String txid) throws Exception {
				System.out.println(); //TODO
			}
		});
		
        org.apache.derby.jdbc.EmbeddedXADataSource derbyDS = new org.apache.derby.jdbc.EmbeddedXADataSource();
        derbyDS.setDatabaseName("db");
        derbyDS.setCreateDatabase("create");	            
        JdbcTransactionalResource derbyResource = new JdbcTransactionalResource("derby", derbyDS);
		
		UserTransactionServiceImp utsi = new UserTransactionServiceImp();
		
		utsi.registerResource(derbyResource);
		
		utsi.registerResource(new RecoverableMSResource("xa/ms1", ms));
		
//        TSInitInfo info = utsi.createTSInitInfo();
        //TODO optionally set config properties on info?
        utsi.init();//info);
		
		UserTransactionManager utm = new UserTransactionManager();

		utm.begin();
		
        Transaction tx = utm.getTransaction();

        tx.enlistResource(ms);

        XAConnection conn = derbyDS.getXAConnection();
        XAResource db = conn.getXAResource();
        tx.enlistResource(db);
		
		//call microservice
		String result = ms.executeInActiveTransaction(new ExecuteCallback<String>() {
			@Override
			public String execute(String txid) throws Exception {
				return "executing " + txid;
			}
		});
		System.out.println(result);

		//call db
		Connection connection = conn.getConnection();
//		connection.prepareStatement("create table temp(id integer)").execute();
		connection.prepareStatement("insert into temp values (1)").execute();
		ResultSet rs = connection.prepareStatement("select count(*) from temp").executeQuery();
		while(rs.next()){
			System.out.println("db: " + rs.getString(1));
		}

		//do we need to delist??
		if(true){
			tx.delistResource(ms, XAResource.TMSUCCESS);
			tx.delistResource(db, XAResource.TMSUCCESS);
			utm.commit();
		}else{
			tx.delistResource(ms, XAResource.TMFAIL);
			tx.delistResource(db, XAResource.TMFAIL);
			utm.rollback();
		}
		
		utsi.shutdown(false);
	}
	
	public static class RecoverableMSResource extends XATransactionalResource {

		private MicroserviceResource ms;

		public RecoverableMSResource(String uniqueName, MicroserviceResource ms) {
			super(uniqueName);
			this.ms = ms;
		}

		@Override
		protected XAResource refreshXAConnection() throws ResourceException {
			return ms;
		}
		
	}
}
