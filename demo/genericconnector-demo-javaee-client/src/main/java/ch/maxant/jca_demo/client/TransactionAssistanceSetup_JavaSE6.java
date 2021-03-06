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
package ch.maxant.jca_demo.client;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import ch.maxant.generic_jca_adapter.TransactionAssistanceFactory;

/** A Java 6 compatible implementation */
@Startup
@Singleton
public class TransactionAssistanceSetup_JavaSE6 {

    @Resource(lookup = "java:/maxant/Acquirer")
    private TransactionAssistanceFactory acquirerFactory;

    @Resource(lookup = "java:/maxant/BookingSystem")
    private TransactionAssistanceFactory bookingFactory;

    @Resource(lookup = "java:/maxant/LetterWriter")
    private TransactionAssistanceFactory letterWriterFactory;

    @PostConstruct
    public void init() {
/* cannot register callbacks twice - either Java SE 6 or 8! See TransactionAssistanceSetup
        acquirerFactory.registerCommitRollbackRecovery(new CommitRollbackRecoveryCallback(){
			@Override
			public String[] getTransactionsInNeedOfRecovery() {
                try {
                    List<String> txids = new AcquirerWebServiceService().getAcquirerPort().findUnfinishedTransactions();
                    txids = txids == null ? new ArrayList<>() : txids;
                    return txids.toArray(new String[0]);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to find transactions requiring recovery for acquirer!", e);
                    return null;
                }
			}

			@Override
			public void commit(String txid) throws Exception {
				new AcquirerWebServiceService()
				.getAcquirerPort().bookReservation(txid);
			}

			@Override
			public void rollback(String txid) throws Exception {
				new AcquirerWebServiceService()
				.getAcquirerPort().cancelReservation(txid);
			}
        	
        });
        
        bookingFactory.registerCommitRollbackRecovery(new CommitRollbackRecoveryCallback(){

			@Override
			public String[] getTransactionsInNeedOfRecovery() {
				//no recovery required, since the resource adapter has been configured to handle this internally
				return null;
			}

			@Override
			public void commit(String txid) throws Exception {
                new BookingSystemWebServiceService()
                .getBookingSystemPort().bookTickets(txid);
			}

			@Override
			public void rollback(String txid) throws Exception {
                new BookingSystemWebServiceService()
                .getBookingSystemPort().cancelTickets(txid);
			}
        });
        
        letterWriterFactory.registerCommitRollbackRecovery(new CommitRollbackRecoveryCallback(){
			@Override
			public String[] getTransactionsInNeedOfRecovery() {
	            //no recovery required, since the resource adapter has been configured to handle this internally
				return null;
			}

			@Override
			public void commit(String txid) throws Exception {
                //noop, since execution was already a commit
			}

			@Override
			public void rollback(String txid) throws Exception {
                new LetterWebServiceService().getLetterWriterPort()
                .cancelLetter(txid);
			}
        });
        
*/
    }

    @PreDestroy
    public void shutdown(){
        acquirerFactory.unregisterCommitRollbackRecovery();
        bookingFactory.unregisterCommitRollbackRecovery();
        letterWriterFactory.unregisterCommitRollbackRecovery();
    }
    
}
