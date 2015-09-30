#genericconnector Standalone Demo with Atomikos

This is a demo of a standalone application which uses Atomikos as its Transaction Manager.

This project contains the following classes:

- `TestAtomikos` - An example of how to use Atomikos. It is not intended that you write your applications like this - use JCA+EJB or Spring instead! There is way too much boilerplate code here. Based on examples found at the Atomikos website.
- `Main` - similar to the above class, but also calls web services. As per usual, the first step is to setup commit and rollback callbacks and the second step is to use the `BasicTransactionAssistanceFactory` to wrap the call to the back end service and bind it into the active transaction. This class does not use JNDI, rather creates a new instance of the `BasicTransactionAssistanceFactoryImpl` which takes care of ensuring that the `TransactionAssistant` which it returns is enlisted into the transaction and delisted when that object is closed. The registration of the commit/rollback callback is handled by the class named `TransactionConfigurator`.  Note how you have to explicitly handle the transaction here by starting and committing it / rolling it back - that is why we recommend using EJB or Spring instead.

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

