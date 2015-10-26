#genericconnector Implementation

This folder contains the source of the actual generic connector resource adapter:

- `genericconnector-parent` - Build using Maven starting from inside this folder.
- `genericconnector-api` - builds the API which your code needs to be dependent on.
- `genericconnector-impl` - contains the Java EE implementation. Not required for Spring/Standalone applications.
- `genericconnector-rar` - assembles the resource adapter used in Java EE environments.
- `genericconnector-javase-common-api` - Additional code used when not in a Java EE environment.
- `genericconnector-atomikos-api` - Additional API code for projects using the Atomikos transaction manager.
- `genericconnector-atomikos-impl` - Additional implementation code for projects using the Atomikos manager.
- `genericconnector-bitronix-api` - Additional API code for projects using the Bitronix transaction manager.
- `genericconnector-bitronix-impl` - Additional implementation code for projects using the Bitronix manager.

For example on how to use this code, please see ../demo/README.md

Otherwise see:

- ../README.md
- http://blog.maxant.co.uk/pebble/2015/08/04/1438716480000.html
- http://blog.maxant.co.uk/pebble/2015/10/05/1444071540000.html

##Usage

See blog article (above) for details, or ../README.md for other examples.

For Java EE, briefly:

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

##Failure During Commit / Rollback

If commit/rollback fails because the backend service temporarily goes down or the network becomes unavailable then the transaction manager will have to re-attempt the commit/rollback.  But each transaction manager does this slightly differently. Here is a summary of what has been observed:

- **JBoss TM** - The response from before the commit/rollback is returned to the client and the error that occurred during commit/rollback is handled internally. Next time there is a recovery, the commit/rollback is re-attempted.
- **Atomikos TM** - Atomikos will keep trying the commit/rollback until it can complete the transaction. Once successful, a `com.atomikos.icatch.HeurHazardException` is thrown, so any successful response before an attempt to commit will be lost.
- **Bitronix TM** - Same as JBoss.

##Configuration in JBoss:
Insert under e.g. `jboss-install/standalone/configuration/standalone.xml`:

        <subsystem xmlns="urn:jboss:domain:resource-adapters:2.0">
            <resource-adapters>
                <resource-adapter id="GenericConnector.rar">
                    <archive>
                    
                        <!-- WATCH OUT FOR THE VERSION NUNBER HERE!! -->
                    
                        genericconnector-demo-javaee-ear-2.1.0.ear#genericconnector-rar-2.1.0.rar
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

##Configuration in Tomcat
See ../demo/genericconnector-demo-tomcat-bitronix or ../demo/genericconnector-demo-tomcat-atomikos for more details.

##Configuration in Jetty
See ../demo/genericconnector-demo-tomcat-bitronix or ../demo/genericconnector-demo-tomcat-atomikos for more details.

##Configuration in Spring Boot
See ../demo/genericconnector-demo-springboot-bitronix or ../demo/genericconnector-demo-springboot-atomikos for more details.

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
