package ch.maxant.generic_jca_adapter;

import javax.resource.spi.ManagedConnectionMetaData;

public class ManagedTransactionAssistanceMetaData implements ManagedConnectionMetaData {

    @Override public String getEISProductName() { return "maxant Generic Resource Adapter"; }

    @Override public String getEISProductVersion() { return "2.0"; }

    @Override public int getMaxConnections() { return 0; }

    @Override public String getUserName() { return null; }

}