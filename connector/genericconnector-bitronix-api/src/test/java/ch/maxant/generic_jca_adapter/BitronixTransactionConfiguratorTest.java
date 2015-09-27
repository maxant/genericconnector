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
		BitronixTransactionConfigurator.setup("a", new CommitRollbackCallback() {
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
		BitronixTransactionConfigurator.unregisterMicroserviceResourceFactory("a");

		assertEquals(0, MicroserviceResourceProducer.getProducers().size());
		assertEquals(0, ResourceRegistrar.getResourcesUniqueNames().size());
	}

}
