/*
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

 */
package ch.maxant.generic_jca_adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * demo of a spring boot application which publishes an endpoint 
 *(see {@link AppController}) and listens for requests.
 *
 * see also:
 *  https://spring.io/guides/gs/rest-service/
 *  https://github.com/spring-projects/spring-boot/blob/master/spring-boot-samples/spring-boot-sample-jta-bitronix/src/main/java/sample/bitronix/SampleBitronixApplication.java
 */
@SpringBootApplication
public class DemoSpringBootServer extends BaseMain {

    public static void main(String[] args) throws Exception {
    	setupCommitRollbackHandlerForMicroserviceWhichIsCalled();

    	
    	//TODO bet this dont work when using two web services? coz atomikos uses a name based on the classname of the resource, rather than the jndi name;
    	
    	//run the application
    	SpringApplication.run(DemoSpringBootServer.class, args);
    }


}
