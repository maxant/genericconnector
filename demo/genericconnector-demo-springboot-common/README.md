#genericconnector Spring Boot Demo Common

This project contains code common to the Atomikos and Bitronix Spring Boot demo applications.

It contains the following classes:

- `Account` - A JPA Entity class representing the `account` table in the database.
- `AppController` - A REST controller which receives `GET` requests at `http://localhost:8191/createUser?username=john`.  This controller passes the call on to the `AppService` described below.
- `AppRepository` - A Spring Data Repository for saving Account entities.
- `AppService` - A Spring service which is transactional due to the class annotation which has `BasicTransactionAssistanceFactory` instances injected for binding two microservice calls (one to the letter service and one to the booking service) into the transaction.  This service first writes to the database and then calls the two services. The Spring container then commits when the method exists.  If the username "john" is sent to the service, it will throw a new exception at the end, and the transaction manager will automatically call the rollback methods on the two services (as well as rollback the database insert).  The two `BasicTransactionAssistanceFactory`s are created in the `Config` class, which is Atomikos/Bitronix specific.  See them under `../genericconnector-demo-springboot-atomikos` / `../genericconnector-demo-springboot-bitronix` respectively.  The database connection is configured in the `application.properties` file also located in the above named projects.

See also:

- http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-jta.html
- https://github.com/spring-projects/spring-boot/tree/master/spring-boot-samples/spring-boot-sample-jta-bitronix
- https://github.com/spring-projects/spring-boot/tree/master/spring-boot-starters/spring-boot-starter-jta-bitronix

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
