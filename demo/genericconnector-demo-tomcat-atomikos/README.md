#genericconnector Standalone Demo with Tomcat + Atomikos

This is a demo of a standard Servlet 3.0 application which can be deployed to Tomcat (or Jetty), which uses Atomikos as its Transaction Manager.

This project contains:

- A class named `CreateUserServlet` - a standard Servlet implementation which can be called via the URL `http://localhost:8081/genericconnector-demo-tomcat-atomikos/sale?account=ant`.  Pass it the account "john" to invoke a failure at the end of the transaction shortly before it is committed.  The `doGet` method handles requests to the URL.  This example has to manage the transaction manually by fetching the user transaction from JNDI. It can do that because the file named `context.xml` (which is Tomcat specific) puts the User Transaction into the JNDI tree. As with other examples, two web services are called within the transaction.  The commit / rollback callback is registered when the servlet context is initialized via the `contextInitialized` method. Note that this method also starts up the Atomikos transaction manager - normally this would NOT be done like this, rather you would startup Atomikos just once for the entire server rather than each application. That requires different configuration and more details can be found at http://www.atomikos.com/Documentation/Tomcat7Integration35. Basically it involves using a Tomcat lifecycle listener to startup the transaction manager when the server starts, and involves putting the relevant libraries in the right places so that the server can find them during startup.

##Notes
- If you deploy to Tomcat from inside Eclipse, then you may need to put the contents of the context.xml into the `Servers/Tomcat/server.xml`.

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

