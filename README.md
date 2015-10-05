#genericconnector - Home

A generic connector capable of binding resources like (microservice) web services into JTA global transactions, **so you don't have to write framework code** to ensure your
data remains consistent when there are system failures.

Imagine calling two web services and the call to the second one fails. You will need to clean up otherwise any data written during the first call will be inconsistent with the missing data that was supposed to be written in the second call which failed!  Instead of writing complex code which can track inconsistencies and repair them, use this library which piggy-backs on top of tried and tested Java transaction managers, to handle recovery automatically.

See details:

- http://blog.maxant.co.uk/pebble/2015/08/04/1438716480000.html
- http://blog.maxant.co.uk/pebble/2015/10/05/1444071540000.html 

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

- `connector` - a folder containing the connector API and implementation,
- `demo` - a folder containing lots of demo projects showing how to use the connector in different environments.

##Sample Code

1) Depend on the API in your Maven POM:

      <dependency>
          <groupId>ch.maxant</groupId>
          <artifactId>genericconnector-api</artifactId>
          <version>2.1.0</version>
      </dependency>

Also see the POMs in the demo applications on Github, because you will also need to provide the relevant implementation modules at runtime.

2) Setting up commit and rollback callbacks:

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

3) Calling a service inside a transaction, so that ANY data written by the service, or indeed using a resource enlisted in the transaction (databases, JMS queues/topics, JCA adapters, etc.) remains globally consistent (all resources and the service commit, or rollback together):

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

##FAQ

See end of http://blog.maxant.co.uk/pebble/2015/08/04/1438716480000.html.

##More advanced setups

1) Before you call your remote service, call a service (EJB/Spring Bean/POJO) which persists the intention to call the remote system, in a new transaction, which commits immediately. This can be used during recovery to tell the remote system how to rollback. It depends on the system, but a good one lets you always send your reference number or a reference number from a previous interaction, so that the context is clear. That way you can tell the remote system to forget what you did during the execution stage. Imagine the execution stage didn't respond and you got a timeout from the remote service. But also imagine that internally, they had completed the execution stage. Because you rollback the transaction after this failure, the transaction manager will call your rollback handler and you can tell the remote system to cancel whatever it was you did when you were executing.

2) Call a service (EJB) which persists the result of the remote system call, in a new transaction, which commits immediately. Persist that info together with the transaction ID, so that you can access the info during commit, rollback or recovery, if you need to. Imagine having to cancel something using the ID that they returned.  That isn't too great a system though, because what if there was simply a timeout and you didn't get the response, but they did complete on their side!  That is why you should perist the intention to call their service - see above.

3) Create cleanup jobs for deleting old data which you persisted in steps 1) or 2) above. EJBs with `@Scheduled` annotations work well.

4) During commit, rollback or recovery, if you need more information than just the transaction ID, do a lookup in your persistent store to find the contextual information you stored in steps 1) or 2).  Once the commit/rollback is successful, you can delete the relevant data from the persistent store.

5) Write a program which generates a report about incomplete transactions, based on the data persisted in steps 1) or 2) and deleted in step 4).  This helps you to sleep well at night, knowing that everything is indeed consistent. Should something be inconsistent, you will then have the information required to fix the inconsistencies.

##Future features
See the issues in Github.

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
