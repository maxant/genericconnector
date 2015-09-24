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
import java.sql.SQLException;

import javax.naming.Context;
import javax.sql.DataSource;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;
import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWriter;

public class Main {

    public static void main(String[] args) throws Exception {
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
        
        String username = "john";

        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();
        BitronixTransaction tx = tm.getCurrentTransaction();
        
        try{//start of service method
	        Context ctx = new BitronixContext();

	        BasicTransactionAssistanceFactory microserviceFactory = (BasicTransactionAssistanceFactory) ctx.lookup("xa/ms1");
	        String msResponse = null;
	        try(TransactionAssistant transactionAssistant = microserviceFactory.getTransactionAssistant()){
	        	msResponse = transactionAssistant.executeInActiveTransaction(txid->{
	        		return service.writeLetter(txid, username);
	        	});
	        }
	        
	        runSql((DataSource) ctx.lookup("jdbc/mysql1"), username);
	        
	        if(username == "john"){
	        	throw new RuntimeException("simulated error");
	        }
	        tx.commit();
	        System.out.println("got " + msResponse + " from microservice");
        }catch(Exception e){
        	e.printStackTrace();
        	tx.rollback();
        }

        //container shutdown
		BitronixTransactionConfigurator.unregisterMicroserviceResourceFactory("xa/ms1");
        tm.shutdown();
    }

    private static void runSql(DataSource ds, String username) throws SQLException {
        try(Connection conn = ds.getConnection()){
        	try(PreparedStatement stmt = conn.prepareStatement("insert into person(id, name) select max(id)+1, ? from person")){
        		stmt.setString(1, username);
        		stmt.executeUpdate();
        	}
        }
    }
}
