#genericconnector Standalone Demo with Tomcat + Bitronix

This is a demo of a standard Servlet 3.0 application which can be deployed to Tomcat (or Jetty), which uses Bitronix as its Transaction Manager.

This project contains:

- A class named `CreateUserServlet` - a standard Servlet implementation which can be called via the URL `http://localhost:8081/genericconnector-demo-tomcat-bitronix/sale?account=ant`.  Pass it the account "john" to invoke a failure at the end of the transaction shortly before it is committed.  The `doGet` method handles requests to the URL.  This example has to manage the transaction manually by fetching the user transaction from JNDI. It can do that because the file named `context.xml` (which is Tomcat specific) puts the User Transaction into the JNDI tree. As with other examples, two web services are called within the transaction.  The commit / rollback callback is registered when the servlet context is initialized via the `contextInitialized` method. Note that this method also starts up the Bitronix transaction manager - normally this would NOT be done like this, rather you would startup Bitronix just once for the entire server rather than each application. That requires different configuration and more details can be found at https://github.com/bitronix/btm. Basically it involves using a Tomcat lifecycle listener (`bitronix.tm.integration.tomcat55.BTMLifecycleListener`) to startup the transaction manager when the server starts, and involves putting the relevant libraries in the right places so that the server can find them during startup.
- A folder named `add-to-tomcat` - contains Bitronix configuration files which must be available to the JVM which looks for a default file named `bitronix-default-config.properties` in the working directory.  The location of that file can be overriden using system properties - see the Bitronix webiste for more information.

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

