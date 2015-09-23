package ch.maxant.generic_jca_adapter;

/** An abstract class capable of binding a microservice call into a transaction. */
public abstract class BitronixTransactionAssistantImpl implements TransactionAssistant {

	private MicroserviceResourceHolder resourceHolder;
	private MicroserviceResourceProducer microserviceResourceProducer;

	public BitronixTransactionAssistantImpl(MicroserviceResourceProducer microserviceResourceProducer, MicroserviceResourceHolder resourceHolder) {
		this.microserviceResourceProducer = microserviceResourceProducer;
		this.resourceHolder = resourceHolder;
	}

	@Override
	public abstract <T> T executeInActiveTransaction(ExecuteCallback<T> c) throws Exception;
	
	@Override
	public void close() {
		microserviceResourceProducer.closeResourceHolder(resourceHolder);
	}
}
