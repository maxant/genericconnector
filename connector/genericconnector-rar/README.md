# genericconnector RAR

This module assembles the API and implementation, to build the RAR used by JCA compatible Java EE servers. Currently no configuration or deployment descriptor lives here (ra.xml, ironjacamar.xml, etc.) since
it is necessary to override the deployment descriptor at assembly time. This is done by the person in the "deployer" role, for example by configuring the resource adapter in the JBoss `standalone.xml` configuration file.

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
