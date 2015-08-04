#genericconnector - Home

A generic JCA resource adapter capable of binding things like (microservice) web services into JTA global transactions controlled by Java EE application servers.

See details at http://blog.maxant.co.uk/pebble/2015/08/04/1438716480000.html

Unlike WS-AT and two phase commit, the idea is that the resources being integrated, like web services, have business operations for **executing** business logic and then **committing** (optional) or **rolling** back the business logic.  If the web service you want to integrate has no commit operation, because for example the execution already commits, then simply leave the implementation of the commit callback empty (see blog for more details).  Transactions will be eventually consistent.

Compatible with Java EE 6 (JCA 1.5) upwards and Java SE 1.6 upwards.
Tested on JBoss EAP 6.2, Wildfly 8.2 and Wildfly 9.0.

See connector/README.md for more details.

This folder contains:
- the "connector" folder, containing the resource adapter
- the "demo" folder containing a demo project showing how to use the adapter

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
