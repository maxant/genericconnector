package ch.maxant.generic_jca_adapter;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.XAConnection;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.atomikos.datasource.xa.jdbc.JdbcTransactionalResource;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;

/** working out how to use atomikos, before building {@link Main} */
public class TestAtomikos {

	public static void main(String[] args) throws Exception {

        MicroserviceXAResource ms = new MicroserviceXAResource("xa/ms1", new UnderlyingConnectionImpl() {
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

        MysqlXADataSource mysql = new MysqlXADataSource();
        mysql.setUser("root");
        mysql.setPassword("password");
        mysql.setUrl("jdbc:mysql://localhost:3306/temp?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=UTF-8");
        JdbcTransactionalResource mysqlResource = new JdbcTransactionalResource("jdbc/mysql", mysql);
		
		UserTransactionServiceImp utsi = new UserTransactionServiceImp();
		
		utsi.registerResource(mysqlResource);
		
		utsi.registerResource(new RecoverableMSResource(ms));
		
        utsi.init();
		
		UserTransactionManager utm = new UserTransactionManager();

		utm.begin();
		
        Transaction tx = utm.getTransaction();

        tx.enlistResource(ms);

        XAConnection xamysql = mysql.getXAConnection();
        XAResource db = xamysql.getXAResource();
        tx.enlistResource(db);
		
        String username = "ant";

        //call microservice
		String result = ms.executeInActiveTransaction(new ExecuteCallback<String>() {
			@Override
			public String execute(String txid) throws Exception {
				return "executing " + txid;
			}
		});
		System.out.println(result);

		//call db
        try(Connection connection = xamysql.getConnection()){
        	try(PreparedStatement stmt = connection.prepareStatement("insert into person(id, name) select max(id)+1, ? from person")){
				stmt.setString(1, username);
        		stmt.executeUpdate();
        	}
        }
		
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
	
}
