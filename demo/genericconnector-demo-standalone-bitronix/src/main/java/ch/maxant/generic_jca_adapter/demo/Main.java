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
package ch.maxant.generic_jca_adapter.demo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.naming.Context;
import javax.sql.DataSource;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;
import bitronix.tm.resource.ResourceLoader;
import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactory;
import ch.maxant.generic_jca_adapter.CommitRollbackCallback;
import ch.maxant.generic_jca_adapter.TransactionAssistant;
import ch.maxant.generic_jca_adapter.TransactionConfigurator;
import ch.maxant.jca_demo.bookingsystem.BookingSystem;
import ch.maxant.jca_demo.bookingsystem.BookingSystemWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWriter;

/**
 * An example of how to use Bitronix. 
 */
public class Main {

    public static void main(String[] args) throws Exception {
    	System.setProperty("log4j.debug", "true");
    	
    	//load up resources registered via bitronix
    	ResourceLoader rl = new ResourceLoader();
    	rl.init();
    	
    	//warning: this instance must be thread safe and serializable. build the web service client on the fly!
        CommitRollbackCallback bookingCommitRollbackCallback = new CommitRollbackCallback() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
				getService().cancelTickets(txid);
			}
			@Override
			public void commit(String txid) throws Exception {
				getService().bookTickets(txid);
			}
			private BookingSystem getService() {
				return new BookingSystemWebServiceService().getBookingSystemPort();
			}
		};

		//warning: this instance must be thread safe and serializable. build the web service client on the fly!
		CommitRollbackCallback letterCommitRollbackCallback = new CommitRollbackCallback() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
				//compensate by cancelling the letter
				LetterWriter service = new LetterWebServiceService().getLetterWriterPort(); //or take from a pool if you want to
				service.cancelLetter(txid);
			}
			@Override
			public void commit(String txid) throws Exception {
				System.out.println("nothing to do, this service autocommits.");
			}
		};

        {//once per microservice that you want to use - do this when app starts, so that recovery can function immediately
        	TransactionConfigurator.setup("xa/bookingService", bookingCommitRollbackCallback);
        	TransactionConfigurator.setup("xa/letterService", letterCommitRollbackCallback);
        }
        
        String username = "ant";

        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();
        BitronixTransaction tx = tm.getCurrentTransaction();
        
        try{//start of service implementation:
	        Context ctx = new BitronixContext();
	        String msResponse = null;

	        //call microservice #1
	        BasicTransactionAssistanceFactory bookingMicroserviceFactory = (BasicTransactionAssistanceFactory) ctx.lookup("xa/bookingService");
	        try(TransactionAssistant transactionAssistant = bookingMicroserviceFactory.getTransactionAssistant()){
	        	msResponse = transactionAssistant.executeInActiveTransaction(txid->{
	        		return new BookingSystemWebServiceService().getBookingSystemPort().reserveTickets(txid, username);
	        	});
	        }
	        
	        //call microservice #2
	        BasicTransactionAssistanceFactory letterMicroserviceFactory = (BasicTransactionAssistanceFactory) ctx.lookup("xa/letterService");
	        try(TransactionAssistant transactionAssistant = letterMicroserviceFactory.getTransactionAssistant()){
	        	msResponse += "/" + transactionAssistant.executeInActiveTransaction(txid->{
	        		return new LetterWebServiceService().getLetterWriterPort().writeLetter(txid, username);
	        	});
	        }

	        //#3 do something with a local db
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
		TransactionConfigurator.unregisterMicroserviceResourceFactory("xa/bookingService");
		TransactionConfigurator.unregisterMicroserviceResourceFactory("xa/letterService");
        tm.shutdown();
        rl.shutdown();
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
