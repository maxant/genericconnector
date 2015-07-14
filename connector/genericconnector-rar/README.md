# genericconnector RAR

This module assembles the API and implementation, to build the RAR. Currently no configuration or deployment descriptor lives here (ra.xml, ironjacamar.xml, etc.) since
it is necessary to override the deployment descriptor at assembly time. This is done by the deployer, for example by configuring the resource adapter in the `server.xml` file.