package ch.maxant.generic_jca_adapter;

import javax.transaction.xa.XAResource;

import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.xa.XATransactionalResource;

public class RecoverableMSResource extends XATransactionalResource {

	private MicroserviceResource ms;

	public RecoverableMSResource(String uniqueName, MicroserviceResource ms) {
		super(uniqueName);
		this.ms = ms;
	}

	@Override
	protected XAResource refreshXAConnection() throws ResourceException {
		return ms;
	}

	MicroserviceResource getMicroserviceResource() {
		return ms;
	}
	
}
