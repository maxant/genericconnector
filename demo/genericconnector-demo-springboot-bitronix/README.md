#genericconnector Spring Boot Demo with Bitronix

Note: this project depends on code found in the `../genericconnector-demo-springboot-common` project.

This project contains the following classes:

- `BaseMain` - an abstract class which the main application inherits from. This class setups up the commit/rollback callback for two services which the application calls. It deregisters the callbacks during shutdown.
- `Config` - This class creates the `BasicTransactionAssistanceFactory` which is injected into the service which calls the two back-end services. See `../genericconnector-demo-springboot-common/src/.../AppService.java` to see how they are injected.  This class is a standard Spring configuration factory bean but you could equally define this bean in Spring's standard XML configuration.
- `DemoSpringBootServerBitronix` - This class contains the main method to start the application. It is based on https://spring.io/guides/gs/rest-service/ and https://github.com/spring-projects/spring-boot/blob/master/spring-boot-samples/spring-boot-sample-jta-bitronix and once you run this main class, you can test the application by calling either http://localhost:8191/createUser?username=ant for a successful case or http://localhost:8191/createUser?username=john for a failure case.

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
