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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.resource.ResourceException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.junit.Before;
import org.junit.Test;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.TransactionContextHelper;

public class MicroserviceResourceProducerTest {

	@Before
	public void setup(){
		//clean up
		for(String name : ResourceRegistrar.getResourcesUniqueNames()){
			MicroserviceResourceProducer.unregisterMicroserviceResourceFactory(name);
		}
	}
	
	@Test
	public void testRegister() {
		assertEquals(0, MicroserviceResourceProducer.getProducers().size());
		assertEquals(0, ResourceRegistrar.getResourcesUniqueNames().size());

		MicroserviceResourceFactory msrFactory = mock(MicroserviceResourceFactory.class);
		MicroserviceXAResource xa = new MicroserviceXAResource("a", null);
		when(msrFactory.build()).thenReturn(xa);

		//TEST
		MicroserviceResourceProducer.registerMicroserviceResourceFactory("a", msrFactory);
		
		assertEquals(1, MicroserviceResourceProducer.getProducers().size());
		MicroserviceResourceProducer producer = MicroserviceResourceProducer.getProducers().values().iterator().next();
		assertEquals("a", producer.getUniqueName());
		assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());
		assertEquals(producer, ResourceRegistrar.get("a"));

		try{
        	//TEST
			MicroserviceResourceProducer.registerMicroserviceResourceFactory("a", mock(MicroserviceResourceFactory.class));
			fail("no exception");
		}catch(IllegalStateException e){
			//OK
		}
		assertEquals(1, MicroserviceResourceProducer.getProducers().size());
		producer = MicroserviceResourceProducer.getProducers().values().iterator().next();
		assertEquals("a", producer.getUniqueName());

    	//TEST
		MicroserviceResourceProducer.unregisterMicroserviceResourceFactory("a");
		assertEquals(0, MicroserviceResourceProducer.getProducers().size());
		assertEquals(0, ResourceRegistrar.getResourcesUniqueNames().size());

    	//TEST
		MicroserviceResourceProducer.unregisterMicroserviceResourceFactory("a");
		assertEquals(0, MicroserviceResourceProducer.getProducers().size());
	}

	@Test
	public void testGetTransactionAssistant() throws ResourceException, NotSupportedException, SystemException {
		MicroserviceResourceFactory msrFactory = mock(MicroserviceResourceFactory.class);
		MicroserviceXAResource xa = new MicroserviceXAResource("a", null);
		when(msrFactory.build()).thenReturn(xa);
		MicroserviceResourceProducer.registerMicroserviceResourceFactory("a", msrFactory);
		MicroserviceResourceProducer producer = MicroserviceResourceProducer.getProducers().values().iterator().next();
		
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        try{
        	tm.begin();

        	//TEST
    		TransactionAssistant ta = producer.getTransactionAssistant();
    		assertNotNull(ta);
    		
    		//check its enlisted in TX
    		assertEquals(1, TransactionContextHelper.currentTransaction().getEnlistedResourcesUniqueNames().size());
    		assertEquals("a", TransactionContextHelper.currentTransaction().getEnlistedResourcesUniqueNames().iterator().next());
    		
        	//TEST
    		ta.close();

    		//cannot check its delisted from TX, because that happens during deconstruction of the transaction, becuase the TX is a global one
    		
    		//close also removed it from the holders - so check its gone
    		assertNull(producer.findXAResourceHolder(xa));
        }finally{
        	tm.rollback();
        }
	}

	@Test
	public void testFind() throws ResourceException, NotSupportedException, SystemException {
		MicroserviceResourceFactory msrFactory = mock(MicroserviceResourceFactory.class);
		MicroserviceXAResource xa = new MicroserviceXAResource("a", mock(CommitRollbackCallback.class));
		when(msrFactory.build()).thenReturn(xa);
		MicroserviceResourceProducer.registerMicroserviceResourceFactory("a", msrFactory);
		MicroserviceResourceProducer producer = MicroserviceResourceProducer.getProducers().values().iterator().next();

        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        try{
        	tm.begin();

        	//TEST
        	producer.getTransactionAssistant(); //enlists resource into TX and means we can then go find it
        	
        	XAResource found = producer.findXAResourceHolder(xa).getXAResource();
        	assertEquals(xa, found);
        }finally{
        	tm.rollback();
        }
	}

	@Test
	public void testRecovery() throws RecoveryException {
		MicroserviceResourceFactory msrFactory = mock(MicroserviceResourceFactory.class);
		when(msrFactory.build()).thenReturn(new MicroserviceXAResource("a", null));
		MicroserviceResourceProducer.registerMicroserviceResourceFactory("a", msrFactory);
		MicroserviceResourceProducer producer = MicroserviceResourceProducer.getProducers().values().iterator().next();
		
		XAResourceHolderState rh = producer.startRecovery();
		assertTrue(rh.getXAResource() instanceof MicroserviceXAResource);
		
		try{
        	//TEST
			rh = producer.startRecovery();
			fail("no exception");
		}catch(RecoveryException e){
			//ok, since recovery is already started
		}
		
    	//TEST
		producer.endRecovery(); //check theres no exception
	}

}
