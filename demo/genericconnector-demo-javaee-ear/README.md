#genericconnector Demo - Java EE EAR

This module builds the demo EAR file which can be deployed to the application server.

See the blog article for more information.

This EAR's POM file can be used as a template, to see how to integrate the resource adapter into your project.

#Notes
- Deployment in Eclipse: doesn't seem to work properly anymore, getting NPE or CDNFE during deployment. Try deploying the EAR that maven builds manually, and ensure that standalone.xml has the correct path name to the RAR, including version numbers etc.  Seems to be a problem with the deployment of the exploded JARs inside the RAR? 20150926, Wildfly 8.2.

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
