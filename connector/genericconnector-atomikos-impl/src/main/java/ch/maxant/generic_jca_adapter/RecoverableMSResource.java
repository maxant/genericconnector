package ch.maxant.generic_jca_adapter;

import javax.transaction.xa.XAResource;

import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.xa.XATransactionalResource;

public class RecoverableMSResource extends XATransactionalResource {

	private MicroserviceXAResource ms;

	public RecoverableMSResource(MicroserviceXAResource ms) {
		super(ms.getJndiName());
		this.ms = ms;
	}

	@Override
	protected XAResource refreshXAConnection() throws ResourceException {
		return ms;
	}

	MicroserviceXAResource getMicroserviceResource() {
		return ms;
	}
	
}
