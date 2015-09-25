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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.atomikos.icatch.config.UserTransactionServiceImp;

public final class AtomikosTransactionConfigurator {

	private AtomikosTransactionConfigurator() {}
	
	private static Map<String, RecoverableMSResource> resources = new HashMap<String, RecoverableMSResource>();
	
	/** one time configuration required for setting up a microservice in a transactional environment. */
	public static void setup(String jndiName, final CommitRollbackCallback commitRollbackCallback){
    	UserTransactionServiceImp utsi = new UserTransactionServiceImp();
        MicroserviceXAResource ms = new MicroserviceXAResourceImpl(jndiName, commitRollbackCallback);
		RecoverableMSResource resource = new RecoverableMSResource(ms);
		resources.put(jndiName, resource);
		utsi.registerResource(resource);
	}

	/** when your application shutsdown, you should unregister all services that were setup using {@link #setup(String, CommitRollbackHandler)} 
	 * or {@link #setup(String, CommitRollbackHandler, long, File)} */
	public static void unregisterMicroserviceResourceFactory(String name) {
    	UserTransactionServiceImp utsi = new UserTransactionServiceImp();
		utsi.removeResource(resources.remove(name));
	}
	
	static CommitRollbackCallback getCommitRollbackCallback(String jndiName){
		return resources.get(jndiName).getMicroserviceResource().getUnderlyingConnection();
	}
	
}
