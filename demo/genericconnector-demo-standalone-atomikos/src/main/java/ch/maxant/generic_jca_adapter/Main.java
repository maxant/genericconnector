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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.atomikos.datasource.xa.jdbc.JdbcTransactionalResource;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;

import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWriter;

public class Main {

    public static void main(String[] args) throws Exception {
        CommitRollbackCallback commitRollbackCallback = new CommitRollbackCallback() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
				//compensate by cancelling the letter
				//warning: this instance must be thread safe and serializable. build the web service client on the fly!
				LetterWriter service = new LetterWebServiceService().getLetterWriterPort(); //or take from a pool if you want to
				service.cancelLetter(txid);
			}
			@Override
			public void commit(String txid) throws Exception {
				System.out.println("nothing to do, this service autocommits.");
			}
		};

        {//once per microservice that you want to use - do this when app starts, so that recovery can function immediately
        	AtomikosTransactionConfigurator.setup("xa/ms1", commitRollbackCallback);
        }

        //setup datasource
        MysqlXADataSource mysql = new MysqlXADataSource();
        mysql.setUser("root");
        mysql.setPassword("password");
        mysql.setUrl("jdbc:mysql://localhost:3306/temp?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=UTF-8");
        JdbcTransactionalResource mysqlResource = new JdbcTransactionalResource("mysql", mysql);
		UserTransactionServiceImp utsi = new UserTransactionServiceImp();
		utsi.registerResource(mysqlResource);
		
		//start TM
        utsi.init();
        
        //start transaction
		UserTransactionManager utm = new UserTransactionManager();
		utm.begin();
        Transaction tx = utm.getTransaction();

        //enlist database connection
        XAConnection xamysql = mysql.getXAConnection();
        XAResource db = xamysql.getXAResource();
        tx.enlistResource(db);
        

        try{//start of service method
        	String username = "ant";
        	
        	//call microservice
	        BasicTransactionAssistanceFactory microserviceFactory = new BasicTransactionAssistanceFactoryImpl("xa/ms1");
	        String msResponse = null;
	        try(TransactionAssistant transactionAssistant = microserviceFactory.getTransactionAssistant()){
	        	msResponse = transactionAssistant.executeInActiveTransaction(txid->{
					LetterWriter service = new LetterWebServiceService().getLetterWriterPort(); //take from pool if you want
	        		return service.writeLetter(txid, username);
	        	});
	        }

			//call db
	        runSql(xamysql, username);
	        
	        if(username == "john"){
	        	throw new RuntimeException("simulated error");
	        }
			tx.delistResource(db, XAResource.TMSUCCESS);
	        tx.commit();
	        System.out.println("got " + msResponse + " from microservice");
        }catch(Exception e){
        	e.printStackTrace();
			tx.delistResource(db, XAResource.TMFAIL);
        	tx.rollback();
        }

        //container shutdown
		AtomikosTransactionConfigurator.unregisterMicroserviceResourceFactory("xa/ms1");
		utsi.shutdown(false);
    }

    private static void runSql(XAConnection xamysql, String username) throws SQLException {
        try(Connection conn = xamysql.getConnection()){
        	try(PreparedStatement stmt = conn.prepareStatement("insert into person(id, name) select max(id)+1, ? from person")){
        		stmt.setString(1, username);
        		stmt.executeUpdate();
        	}
        }
    }
}
