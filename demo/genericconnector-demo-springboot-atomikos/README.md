#genericconnector Spring Boot Demo with Atomikos

Note: this project depends on code found in the `../genericconnector-demo-springboot-common` project.

This project contains the following classes:

- `BaseMain` - an abstract class which the main application inherits from. This class setups up the commit/rollback callback for two services which the application calls. It deregisters the callbacks during shutdown.
- `Config` - This class creates the `BasicTransactionAssistanceFactory` which is injected into the service which calls the two back-end services. See `../genericconnector-demo-springboot-common/src/.../AppService.java` to see how they are injected.  This class is a standard Spring configuration factory bean but you could equally define this bean in Spring's standard XML configuration.
- `DemoSpringBootServerAtomikos` - This class contains the main method to start the application. It is based on https://spring.io/guides/gs/rest-service/ and https://github.com/spring-projects/spring-boot/blob/master/spring-boot-samples/spring-boot-sample-jta-bitronix and once you run this main class, you can test the application by calling either http://localhost:8191/createUser?username=ant for a successful case or http://localhost:8191/createUser?username=john for a failure case.

##Notes

- Watch out, transaction logs end up in maven repo, e.g. `/shared/local-maven-repo/org/springframework/boot/spring-boot/1.2.6.RELEASE/transaction-logs/tmlog-1.log` - if during development you change lots of stuff and tx logs are no longer compatible, delete them before restarting the app, to avoid lots of error messages in the logs.
- If commit/rollback fails because the backend service temporarily goes down then Atomikos will keep trying but won't respond to the client with a success until it can complete the transaction. This is different from JBoss and Bitronix.  Atomikos will eventually respond with an Exception: `com.atomikos.icatch.HeurHazardException`.
- SpringData, Atomikos and Mysql might not work nicely - according to Atomikos this is a problem with Mysql. According to Mysql this calling `XA START JOIN` is not legal. 
    
    WARN c.a.datasource.xa.XAResourceTransaction  : XAResource.start ( ...txid... , XAResource.TMJOIN ) on resource dataSource represented by XAResource instance com.mysql.jdbc.jdbc2.optional.JDBC4MysqlXAConnection@114f3b08
    WARN c.a.datasource.xa.XAResourceTransaction  : XA resource 'dataSource': resume for XID '...txid...' raised -5: invalid arguments were given for the XA operation
    ...
    com.mysql.jdbc.jdbc2.optional.MysqlXAException: XAER_INVAL: Invalid arguments (or unsupported command)

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
