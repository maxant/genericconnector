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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import bitronix.tm.resource.ResourceRegistrar;

public class BitronixTransactionConfiguratorTest {

	@Before
	public void setup(){
		//clean up
		for(String name : ResourceRegistrar.getResourcesUniqueNames()){
			MicroserviceResourceProducer.unregisterMicroserviceResourceFactory(name);
		}
	}

	@Test
	public void test() {
		assertEquals(0, MicroserviceResourceProducer.getProducers().size());
		assertEquals(0, ResourceRegistrar.getResourcesUniqueNames().size());

		//TEST
		TransactionConfigurator.setup("a", new CommitRollbackCallback() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rollback(String txid) throws Exception {
			}
			@Override
			public void commit(String txid) throws Exception {
			}
		});

		assertEquals(1, MicroserviceResourceProducer.getProducers().size());
		assertEquals("a", MicroserviceResourceProducer.getProducers().keySet().iterator().next());
		assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());
		
		
		//TEST
		TransactionConfigurator.unregisterMicroserviceResourceFactory("a");

		assertEquals(0, MicroserviceResourceProducer.getProducers().size());
		assertEquals(0, ResourceRegistrar.getResourcesUniqueNames().size());
	}

}
