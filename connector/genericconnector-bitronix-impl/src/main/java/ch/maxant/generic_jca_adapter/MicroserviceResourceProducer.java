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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.resource.ResourceException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.TransactionContextHelper;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.resource.ehcache.EhCacheXAResourceProducer;

/** based on {@link EhCacheXAResourceProducer} */
class MicroserviceResourceProducer extends ResourceBean implements XAResourceProducer, BasicTransactionAssistanceFactory {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(MicroserviceResourceProducer.class.getName());

    private static final ConcurrentMap<String, MicroserviceResourceProducer> producers =
            new ConcurrentHashMap<String, MicroserviceResourceProducer>();

    private volatile RecoveryXAResourceHolder recoveryXAResourceHolder;

	private MicroserviceResourceFactory msrFactory;

	private ConcurrentMap<XAResource, MicroserviceResourceHolder> resourceHolders = new ConcurrentHashMap<XAResource, MicroserviceResourceHolder>();

    private MicroserviceResourceProducer() {
        setApplyTransactionTimeout(true);
    }

    /**
     * Register a Microservice (XA)Resource factory with BTM. 
     * 
     * @param uniqueName
     *            the uniqueName of the microservice
     */
    public static void registerMicroserviceResourceFactory(String uniqueName, MicroserviceResourceFactory msrFactory) {
    	MicroserviceResourceProducer msResourceProducer = producers.get(uniqueName);
        if (msResourceProducer == null) {
            msResourceProducer = new MicroserviceResourceProducer();
            msResourceProducer.setUniqueName(uniqueName);
            msResourceProducer.setFactory(msrFactory);

            producers.put(uniqueName, msResourceProducer);
            msResourceProducer.init();
        } else {
        	throw new IllegalStateException("already added a factory with the name " + uniqueName + "!");
        }
    }

    private void setFactory(MicroserviceResourceFactory msrFactory) {
		this.msrFactory = msrFactory;
	}

	/**
     * Unregister a microservice resource factory from BTM.
     */
    public static void unregisterMicroserviceResourceFactory(String uniqueName) {
    	MicroserviceResourceProducer msResourceProducer = producers.remove(uniqueName);

        if (msResourceProducer != null) {
            msResourceProducer.setFactory(null);
            msResourceProducer.close();
        } else {
            log.error("no MicroserviceResource Factory registered with name " + uniqueName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XAResourceHolderState startRecovery() throws RecoveryException {
        if (recoveryXAResourceHolder != null) {
            throw new RecoveryException("recovery already in progress on " + this);
        }

        if (msrFactory == null) {
            throw new RecoveryException("no MicroserviceResource Factory registered, recovery cannot be done on " + this);
        }

        recoveryXAResourceHolder = new RecoveryXAResourceHolder(new MicroserviceResourceHolder(msrFactory.build(), this));
        return new XAResourceHolderState(recoveryXAResourceHolder, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endRecovery() throws RecoveryException {
        recoveryXAResourceHolder = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFailed(boolean failed) {
        // microservices dont support this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MicroserviceResourceHolder findXAResourceHolder(XAResource xaResource) {
    	return this.resourceHolders.get(xaResource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        try {
            ResourceRegistrar.register(this);
        } catch (RecoveryException ex) {
            throw new BitronixRuntimeException("error recovering " + this, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        ResourceRegistrar.unregister(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MicroserviceResourceHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reference getReference() throws NamingException {
        return new Reference(MicroserviceResourceProducer.class.getName(), new StringRefAddr("uniqueName", getUniqueName()),
                ResourceObjectFactory.class.getName(), null);
    }

    @Override
    public String toString() {
        return "a MicroserviceResourceProducer with uniqueName " + getUniqueName();
    }

    @Override
    public TransactionAssistant getTransactionAssistant() throws ResourceException {
    	final MicroserviceXAResource microserviceResource = msrFactory.build();
    	final MicroserviceResourceHolder resourceHolder = new MicroserviceResourceHolder(microserviceResource, this);
    	resourceHolders.put(resourceHolder.getXAResource(), resourceHolder);
        try {
        	TransactionContextHelper.enlistInCurrentTransaction(resourceHolder);
		} catch (Exception e) {
			throw new ResourceException("Unable to enlist resource into transaction", e);
		}
        return new TransactionAssistant() {
			@Override
			public <T> T executeInActiveTransaction(ExecuteCallback<T> c) throws Exception {
				return microserviceResource.executeInActiveTransaction(c);
			}
			@Override
			public void close() {
	        	try {
					TransactionContextHelper.delistFromCurrentTransaction(resourceHolder);
				} catch (SystemException e) {
					throw new RuntimeException(e);
				}
	        	resourceHolders.remove(resourceHolder.getXAResource());
			}
		};
    }
    
    /** meant for testing only */
    static ConcurrentMap<String, MicroserviceResourceProducer> getProducers() {
		return producers;
	}
}
