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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.Context;
import javax.sql.DataSource;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;
import bitronix.tm.resource.ResourceLoader;
import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWriter;

public class Main {

	//TODO tomcat + bitronix: http://bitronix-transaction-manager.10986.n7.nabble.com/tomcat-7-0-26-and-bitronix-td1155.html
	//TODO spring boot + bitronix: https://github.com/spring-projects/spring-boot/blob/master/spring-boot-samples/spring-boot-sample-jta-bitronix/src/main/java/sample/bitronix/SampleBitronixApplication.java
	
    public static void main(String[] args) throws Exception {
        ResourceLoader rl = new ResourceLoader();
        rl.init();

        final LetterWriter service = new LetterWebServiceService().getLetterWriterPort(); //take from pool if you want
        CommitRollbackHandler commitRollbackCallback = new CommitRollbackHandler() {
			@Override
			public void rollback(String txid) throws Exception {
				//compensate by cancelling the letter
				service.cancelLetter(txid);
			}
			@Override
			public void commit(String txid) throws Exception {
				//nothing to do, this service autocommits.
			}
		};

        {//once per microservice that you want to use - do this when app starts, so that recovery can function immediately
        	BitronixTransactionConfigurator.setup("xa/ms1", commitRollbackCallback, 30000L, new File("."));
        }

        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        {//start of service method
	        Context ctx = new BitronixContext();
	        DataSource ds = (DataSource) ctx.lookup("jdbc/mysql1");
	
	        tm.begin();
	        BitronixTransaction tx = tm.getCurrentTransaction();

	        BasicTransactionAssistanceFactory microserviceFactory = (BasicTransactionAssistanceFactory) ctx.lookup("xa/ms1");
	        String msResponse = null;
	        try(TransactionAssistant transactionAssistant = microserviceFactory.getTransactionAssistant()){
	        	msResponse = transactionAssistant.executeInActiveTransaction(txid->{
	        		return service.writeLetter(txid, "someReferenceNumber");
	        	});
	        }
	        
	        runSql(ds);
	
	        tx.rollback(); //commit();
	        
	        System.err.println("got " + msResponse + " from microservice");
        }

        //container shutdown
		BitronixTransactionConfigurator.unregisterMicroserviceResourceFactory("xa/ms1");
        rl.shutdown();
        tm.shutdown();
    }

    private static void runSql(DataSource ds) throws SQLException {
        Connection conn = ds.getConnection();
        PreparedStatement stmt = conn.prepareStatement("select 1 from dual");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            System.out.println(rs.getString(1));
        }
        rs.close();
        stmt.close();
        conn.close();
    }
}
