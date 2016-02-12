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


import static ch.maxant.jca_demo.client.IntegrationLayer.getAcquirer;
import static ch.maxant.jca_demo.client.IntegrationLayer.getBookingsystem;
import static ch.maxant.jca_demo.client.IntegrationLayer.getLetterwriter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import ch.maxant.generic_jca_adapter.TransactionAssistanceFactory;
import ch.maxant.generic_jca_adapter.TransactionAssistanceFactory.CommitRollbackRecoveryCallback.Builder;

@Startup
@Singleton
public class TransactionAssistanceSetup {

    private final Logger log = Logger.getLogger(this.getClass().getName());

    @Resource(lookup = "java:/maxant/Acquirer")
    private TransactionAssistanceFactory acquirerFactory;

    @Resource(lookup = "java:/maxant/BookingSystem")
    private TransactionAssistanceFactory bookingFactory;

    @Resource(lookup = "java:/maxant/LetterWriter")
    private TransactionAssistanceFactory letterWriterFactory;

    @PostConstruct
    public void init() {
    	log.log(Level.INFO, "Registering commit/rollback/recovery callbacks");
        acquirerFactory
            .registerCommitRollbackRecovery(new Builder()
            .withCommit( txid -> {
                getAcquirer().bookReservation(txid);
            })
            .withRollback( txid -> {
            	getAcquirer().cancelReservation(txid);
            })
            .withRecovery( () -> {
                try {
                    List<String> txids = getAcquirer().findUnfinishedTransactions();
                    txids = txids == null ? new ArrayList<>() : txids;
                    return txids.toArray(new String[0]);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to find transactions requiring recovery for acquirer!", e);
                    return null;
                }
            }).build());
        
        bookingFactory
            .registerCommitRollbackRecovery(new Builder()
            .withCommit( txid -> {
            	getBookingsystem().bookTickets(txid);
            })
            .withRollback( txid -> {
            	getBookingsystem().cancelTickets(txid);
            })
            //no recovery required, since the resource adapter has been configured to handle this internally
            .build());
        
        letterWriterFactory
            .registerCommitRollbackRecovery(new Builder()
            .withCommit( txid -> {
                //noop, since execution was already a commit
            })
            .withRollback( txid -> {
            	getLetterwriter().cancelLetter(txid);
            })
            //no recovery required, since the resource adapter has been configured to handle this internally
            .build());
        
    }

    @PreDestroy
    public void shutdown(){
    	log.log(Level.INFO, "Unregistering commit/rollback/recovery callbacks");
        acquirerFactory.unregisterCommitRollbackRecovery();
        bookingFactory.unregisterCommitRollbackRecovery();
        letterWriterFactory.unregisterCommitRollbackRecovery();
    }
    
}
