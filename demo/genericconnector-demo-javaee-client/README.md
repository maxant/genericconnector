#genericconnector Demo - Java EE Client

This module contains 5 main source files, as follows.

`SomeServiceThatBindsResourcesIntoTransaction.java` => This file represents a service which binds multiple resources into a global transaction and shows how to call web services from your business code, using Java 8.

`SomeServiceThatBindsResourcesIntoTransaction_JavaSE6.java` => This file represents a service which binds multiple resources into a global transaction and shows how to call web services from your business code, using Java 6.

`TransactionAssistanceSetup.java` => This file shows how to register commit, rollback and recover callbacks, using Java 8.

`TransactionAssistanceSetup_JavaSE6.java` => This file shows how to register commit, rollback and recover callbacks, using Java 6.

`ResourceServlet.java` => This file is a simple servlet which calls the service which binds multiple resources into a single global transaction. It is used for testing.

##Tests from your browser:

- http://localhost:8080/genericconnectordemo/ResourceServlet => positive test case with everything committed
- http://localhost:8080/genericconnectordemo/ResourceServlet?refNum=FAILDB => foreign key constraint violation causes failure at end of process, everything is rolled back
- http://localhost:8080/genericconnectordemo/ResourceServlet?refNum=FAILWSAcquirer =>  the acquirer fails during execution
- http://localhost:8080/genericconnectordemo/ResourceServlet?refNum=FAILWSBookingSystem =>  the booking system fails during execution
- http://localhost:8080/genericconnectordemo/ResourceServlet?refNum=FAILWSLetterWriter =>  the letter writer fails during execution

##Other tests:
- Add a breakpoint during commit and undeploy the web services => eventually, when you redeploy the web services, the transactions will be commited.
- Add a breakpoint during commit and kill the database. => the transactions in the web services are committed.  After restarting the database, the transaction should be commit. Watch out on Mysql versions prior to 5.7!
- Add a breakpoint during commit, and kill the server. After server restart, the web services become committed (wait 2 minutes for JBoss to run recovery!). Database also.

##Places to check for transaction state:

- The database, in the `temp.person` and `temp.address` tables => one row is written per call to the servlet
- In the folder jboss/standalone/data/bookingsystem-tx-object-store/ => either a file name exec...txt exists and the transaction is incomplete, or the file has been deleted.
- In the folder jboss/standalone/data/letterwriter-tx-object-store/ => either a file name exec...txt exists and the transaction is incomplete, or the file has been deleted.
- In the folder ~/temp/xa-transactions-state-acquirer/ => file named exectxt exists if transaction is incomplete; file named commit...txt exists if transaction was committed; file named rollback...txt exists if transaction was rolled back.

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
