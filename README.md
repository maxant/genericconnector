#genericconnector - Home

A generic connector capable of binding resources like (microservice) web services into JTA global transactions, **so you don't have to write framework code** to ensure your
data remains consistent.

Imagine calling two web services and the call to the second one fails. You will need to clean up otherwise any data written during the first call will be inconsistent with the missing data that was supposed to be written in the second call which failed!  Instead of writing complex code which can track inconsistencies and repair them, use this library which piggy-backs on top of tried and tested Java transaction managers, to handle recovery automatically.

See details:

- http://blog.maxant.co.uk/pebble/2015/08/04/1438716480000.html
- TODO

Unlike WS-AT and two phase commit, the idea is that the resources being integrated, like web services, have business operations for **executing** business logic and then **committing** (optional) or **rolling** back the business logic.  If the web service you want to integrate has no commit operation, because for example the execution already commits, then simply leave the implementation of the commit callback empty (see blog for more details).  In all cases, **transactions will be eventually consistent**.

##Compatability

- Java EE 6 (JCA 1.5) upwards and Java SE 1.6 upwards, i.e. JBoss, WebSphere, etc.,
- Spring 3? and 4,
- Spring Boot 1.2.6 upwards,
- Bitronix 2.1.4 upwards,
- Atomikos 3.9.3 upwards,
- Tomcat,
- Jetty,
- Spring Batch,
- Standalone Java Applications

Tested on JBoss EAP 6.2, Wildfly 8.2 and Wildfly 9.0, Spring Boot 1.2.6, Tomcat 7.

##More Info

See connector/README.md for more details.

See demo/README.md for lots of examples!

This folder contains:

- the "connector" folder, containing the connector
- the "demo" folder containing lots of demo projects showing how to use the connector in different environments.

##Sample Code

1) Setting up commit and rollback callbacks:

      CommitRollbackCallback bookingCommitRollbackCallback = new CommitRollbackCallback() {
          public void rollback(String txid) throws Exception {
              new BookingSystemWebServiceService().getBookingSystemPort().cancelTickets(txid);
          }
          public void commit(String txid) throws Exception {
              //optional, if the method called below is auto-committing
              new BookingSystemWebServiceService().getBookingSystemPort().bookTickets(txid);
          }
      };
      TransactionConfigurator.setup("xa/bookingService", bookingCommitRollbackCallback);

2) Calling a service inside a transaction, so that ANY data written using a resource enlisted in the transaction (databases, JMS queues/topics, JCA adapters, etc.) remains consistent:

      @Inject BasicTransactionAssistanceFactory bookingServiceFactory;

      @Transactional
      public String doSomethingWithAGlobalTransactionAndARemoteMicroservice(String username) throws Exception {
      ...
          //call a database or JMS queue; do some business logic...
      ...
          //call to remote (micro)service
          try(TransactionAssistant transactionAssistant = bookingServiceFactory.getTransactionAssistant()){
              msResponse = transactionAssistant.executeInActiveTransaction(txid->{

                  //This is a SOAP Web Service. But it could be a JSON REST Service too. Or indeed anything e.g. a remote file system.
                  BookingSystem service = new BookingSystemWebServiceService().getBookingSystemPort();

                  return service.reserveTickets(txid, username);
              });
          }
      ...
          //call a database or JMS queue; do some business logic...
      ...


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
