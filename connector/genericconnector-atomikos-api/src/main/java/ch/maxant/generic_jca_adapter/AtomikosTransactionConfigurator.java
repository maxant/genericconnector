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
	
	/** one time configuration required for setting up a microservice in a transactional environment. uses default of 30 seconds
     * minAgeOfTransactionInMSBeforeRelevantForRecovery and the current directory for the recoveryStatePersistenceDirectory. */
	public static void setup(String jndiName, CommitRollbackHandler commitRollbackCallback){
		setup(jndiName, commitRollbackCallback, 30000L, new File("."));
	}

	/** one time configuration required for setting up a microservice in a transactional environment */
	public static void setup(String jndiName, final CommitRollbackHandler commitRollbackHandler, long minAgeOfTransactionInMSBeforeRelevantForRecovery, File recoveryStatePersistenceDirectory){
    	MicroserviceResource.configure(minAgeOfTransactionInMSBeforeRelevantForRecovery, recoveryStatePersistenceDirectory);
    	UserTransactionServiceImp utsi = new UserTransactionServiceImp();
        MicroserviceResource ms = new MicroserviceResource(commitRollbackHandler);
		RecoverableMSResource resource = new RecoverableMSResource(jndiName, ms);
		resources.put(jndiName, resource);
		utsi.registerResource(resource);
	}

	/** when your application shutsdown, you should unregister all services that were setup using {@link #setup(String, CommitRollbackHandler)} 
	 * or {@link #setup(String, CommitRollbackHandler, long, File)} */
	public static void unregisterMicroserviceResourceFactory(String name) {
    	UserTransactionServiceImp utsi = new UserTransactionServiceImp();
		utsi.removeResource(resources.remove(name));
	}
	
	static CommitRollbackHandler getHandler(String jndiName){
		return resources.get(jndiName).getMicroserviceResource().getCommitRollbackHandler();
	}
	
}
