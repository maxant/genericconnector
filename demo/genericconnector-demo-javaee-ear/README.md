#genericconnector Demo - EAR

This module builds the demo EAR file which can be deployed to the application server.

See the blog article for more information.

This EAR's POM file can be used as a template, to see how to integrate the resource adapter into your project.

#Notes
- Deployment in Eclipse: doesnt seem to work properly, getting NPE or CDNFE. Try deploying the EAR that maven builds, and ensure that standalone.xml has the correct
path name to the RAR, including version numbers etc.  Seems to be a problem with the deployment of the exploded JARs inside the RAR? 20150926, Wildfly 8.2.