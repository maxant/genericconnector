#genericconnector

This folder contains the source of the actual generic connector resource adapter.

Build using Maven starting from genericconnector-parent.

The code here builds a generic JCA resource adapter capable of binding things like web services into JTA global transactions controlled by Java EE application servers.

See details at http://blog.maxant.co.uk/pebble/2015/08/04/1438716480000.html

Unlike WS-AT the idea is that the resources being integrated, like web services, have business operations for executing business logic and then committing or rolling
back the business logic.  If the web service you want to integrate has no commit operation, because for example the execution already does it, then simply leave the 
implementation of the commit callback empty (see below).  Transactions will be eventually consistent.

Compatible with Java EE 6 (JCA 1.5) upwards and Java SE 1.6 upwards.
Tested on JBoss EAP 6.2, Wildfly 8.2 and Wildfly 9.0.

##Usage

See blog article (above) for details.

Briefly:

1) Register commit and rollback callbacks:

    @Startup
    @Singleton
    public class TransactionAssistanceSetup {
    ...
      // 1) inject factory from generic resource adapter    
      @Resource(lookup = "java:/maxant/BookingSystem")
      private TransactionAssistanceFactory bookingFactory;
    ...
      @PostConstruct
      public void init() {
        // 2) register callbacks
        bookingFactory
          .registerCommitRollbackRecovery(new Builder()
          .withCommit( txid -> {
            new BookingSystemWebServiceService()
              .getBookingSystemPort().bookTickets(txid);
          })
          .withRollback( txid -> {
            new BookingSystemWebServiceService()
              .getBookingSystemPort().cancelTickets(txid);
          })
          .build());
        ...
      }
    
      @PreDestroy
      public void shutdown(){
          // 3) unregister
          bookingFactory.unregisterCommitRollbackRecovery();
          ...
      }
    

2) Call a web service from some business code:

    @Stateless
    public class SomeServiceThatBindsResourcesIntoTransaction {

      // 1) inject factory from generic resource adapter    
      @Resource(lookup = "java:/maxant/BookingSystem")
      private TransactionAssistanceFactory bookingFactory;
    ...
      //TRANSACTION START
      public String doSomethingInvolvingSeveralResources(String refNumber) {
    ...
        // 2) instantiate a web service client
        BookingSystem bookingSystem = new BookingSystemWebServiceService().getBookingSystemPort();
    ...
        // 3) get assistant from factory
        try ( ...
          TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
    ... ) {
          // 4) call web service in transaction
          String bookingResponse = bookingTransactionAssistant.executeInActiveTransaction(txid -> {
            return bookingSystem.reserveTickets(txid, refNumber);
          });
        ...  
      }    
      //TRANSACTION END
    
##FAQ

See end of blog article.

##More advanced setups

1) Call a service (EJB) which persists the intention to call the remote system, in a new transaction, which commits immediately. This can be used during recovery to tell the remote system to rollback. It depends on the system, but a good one lets you always send your reference number or a reference number from a previous interaction, so that the context is clear. That way you can tell the remote system to forget what you did during the execution stage. Imagine the execution stage didn't response and you got a timeout from the remote service. But also imagine that internally, they had completed the execution stage. Because you rollback the transaction after this failure, the transaction manager will call your rollback handler and you can tell the remote system to cancel whatever it was you did when you were executing.

2) Call a service (EJB) which persist the result of the remote system call, in a new transaction, which commits immediately. Persist that info together with the transaction ID, so that you can access the info during commit, rollback or recovery, if you need to. Imagine having to cancel something using the ID that they returned.  That isn't too great a system though, because what if there was simply a timeout and you didn't get the response, but they did complete on their side!  That is why you should perist the intention to call their service - see above.

3) Create cleanup jobs for deleting old data which you persisted in steps 1) or 2) above. EJBs with `@Scheduled` annotations work well.

4) During commit, rollback or recovery, if you need more information than just the transaction ID, do a lookup in your persistent store to find the contextual information you stored in steps 1) or 2).  Once the commit/rollback is successful, you can delete the relevant data from the persistent store.

5) Write a program which generates a report about incomplete transactions, based on the data persisted in steps 1) or 2) and deleted in step 4).  This helps you to sleep well at night, knowing that everything is indeed consistent. Should something be inconsistent, you will then have the information required to fix the inconsistencies.

##Future features
- `commit` and `rollback` could provide a parameter named `attemptNumber` which tells the application code how many times it has attempted to commit or rollback, so that the application code can decided to put the transaction into a dead letter queue if it wants to. 

##Configuration in JBoss:
Insert under e.g. `jboss-install/standalone/configuration/server.xml`:

        <subsystem xmlns="urn:jboss:domain:resource-adapters:2.0">
            <resource-adapters>
                <resource-adapter id="GenericConnector.rar">
                    <archive>
                    
                        <!-- WATCH OUT FOR THE VERSION NUNBER HERE!! -->
                    
                        genericconnector-demo-ear.ear#genericconnector-rar-2.0.0-SNAPSHOT.rar
                    </archive>
                    <transaction-support>XATransaction</transaction-support>
                    <connection-definitions>
                        <connection-definition class-name="ch.maxant.generic_jca_adapter.ManagedTransactionAssistanceFactory" jndi-name="java:/maxant/Acquirer" pool-name="AcquirerPool">
                            <config-property name="id">
                                Acquirer
                            </config-property>
                            <xa-pool>
                                <min-pool-size>1</min-pool-size>
                                <max-pool-size>5</max-pool-size>
                            </xa-pool>
                            <recovery no-recovery="false">
                                <recover-credential>
                                    <user-name>asdf</user-name>
                                    <password>fdsa</password>
                                </recover-credential>
                            </recovery>
                        </connection-definition>
                        <connection-definition class-name="ch.maxant.generic_jca_adapter.ManagedTransactionAssistanceFactory" jndi-name="java:/maxant/BookingSystem" pool-name="BookingSystemPool">
                            <config-property name="recoveryStatePersistenceDirectory">
                                ../standalone/data/bookingsystem-tx-object-store
                            </config-property>
                            <config-property name="id">
                                BookingSystem
                            </config-property>
                            <config-property name="handleRecoveryInternally">
                                true
                            </config-property>
                            <xa-pool>
                                <min-pool-size>1</min-pool-size>
                                <max-pool-size>5</max-pool-size>
                            </xa-pool>
                            <recovery no-recovery="false">
                                <recover-credential>
                                    <user-name>asdf</user-name>
                                    <password>fdsa</password>
                                </recover-credential>
                            </recovery>
                        </connection-definition>
                        <connection-definition class-name="ch.maxant.generic_jca_adapter.ManagedTransactionAssistanceFactory" jndi-name="java:/maxant/LetterWriter" pool-name="LetterWriterPool">
                            <config-property name="recoveryStatePersistenceDirectory">
                                ../standalone/data/letterwriter-tx-object-store
                            </config-property>
                            <config-property name="id">
                                LetterWriter
                            </config-property>
                            <config-property name="handleRecoveryInternally">
                                true
                            </config-property>
                            <xa-pool>
                                <min-pool-size>1</min-pool-size>
                                <max-pool-size>5</max-pool-size>
                            </xa-pool>
                            <recovery no-recovery="false">
                                <recover-credential>
                                    <user-name>asdf</user-name>
                                    <password>fdsa</password>
                                </recover-credential>
                            </recovery>
                        </connection-definition>
                    </connection-definitions>
                </resource-adapter>
            </resource-adapters>
        </subsystem>


##Configuration in Websphere

TODO

##Configuration in Glassfish

TODO

##Configuration in TomEE

TODO

##Configuration in JOnAS

TODO

##Configuration in other Java EE application servers

TODO



##License

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
