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

import ch.maxant.generic_jca_adapter.BitronixTransactionConfigurator;
import ch.maxant.generic_jca_adapter.CommitRollbackCallback;
import ch.maxant.jca_demo.bookingsystem.BookingSystemWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;

public abstract class BaseMain {

	protected static void setupCommitRollbackHandlerForMicroserviceWhichIsCalled() {
    	{//setup microservice that we want to call within a transaction
    		CommitRollbackCallback bookingCommitRollbackCallback = new CommitRollbackCallback() {
				private static final long serialVersionUID = 1L;
				@Override
    			public void rollback(String txid) throws Exception {
    				new BookingSystemWebServiceService().getBookingSystemPort().cancelTickets(txid);
    			}
    			@Override
    			public void commit(String txid) throws Exception {
    				new BookingSystemWebServiceService().getBookingSystemPort().bookTickets(txid);
    			}
    		};
    		BitronixTransactionConfigurator.setup("xa/bookingService", bookingCommitRollbackCallback);

    		CommitRollbackCallback letterCommitRollbackCallback = new CommitRollbackCallback() {
    			private static final long serialVersionUID = 1L;
    			@Override
    			public void rollback(String txid) throws Exception {
    				//compensate by cancelling the letter
    				new LetterWebServiceService().getLetterWriterPort().cancelLetter(txid);
    			}
    			@Override
    			public void commit(String txid) throws Exception {
    				//nothing to do, this service autocommits.
    			}
    		};
    		BitronixTransactionConfigurator.setup("xa/letterService", letterCommitRollbackCallback);
    	}

    	//when app shutsdown, we want to deregister the microservice from bitronix's singleton transaction manager.
    	//this is important in say tomcat, where applications can be installed/uninstalled, but bitronix's lifecycle 
    	//depends on the server, not individual applications.
		Runtime.getRuntime().addShutdownHook(new Thread(){
    		@Override
    		public void run() {
    			//shutdown
    			BitronixTransactionConfigurator.unregisterMicroserviceResourceFactory("xa/bookingService");
    			BitronixTransactionConfigurator.unregisterMicroserviceResourceFactory("xa/letterService");
    		}
    	});
	
	}
	
}
