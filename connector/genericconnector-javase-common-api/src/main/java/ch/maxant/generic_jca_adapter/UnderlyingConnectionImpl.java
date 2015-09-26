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
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public abstract class UnderlyingConnectionImpl implements UnderlyingConnection, Serializable {

	private static final long serialVersionUID = 1L;

	private Boolean wasExecuteSuccessful = null;
    private String currentTxId = null;

    /** Calls the callback to do some work bound into the transaction. 
     * Tracks transaction state internally if necessary. */
    protected <O> O execute(ExecuteCallback<O> c, File recoveryStatePersistenceDirectory) throws Exception {
        if(currentTxId == null) throw new IllegalStateException("XID not yet set - was transaction started?");
        if(wasExecuteSuccessful != null) throw new IllegalStateException("not closed?");
        
        persistTransactionState(recoveryStatePersistenceDirectory);
        
        try{
            O o = c.execute(currentTxId);
            wasExecuteSuccessful = true;
            return o;
        }catch(Exception e) {
            wasExecuteSuccessful = false;
            throw e;
        }
    }

    private void persistTransactionState(File recoveryStatePersistenceDirectory) throws IOException {
        Files.write(File.createTempFile("exec.", ".txt", recoveryStatePersistenceDirectory).toPath(), currentTxId.getBytes(StandardCharsets.UTF_8));
    }
    
	@Override
	public boolean wasExecuteSuccessful() {
		return wasExecuteSuccessful;
	}

	@Override
	public void cleanup() {
		currentTxId = null;
		wasExecuteSuccessful = null;
	}

	@Override
	public void setCurrentTxId(String txid) {
		this.currentTxId = txid;
	}

	@Override
	public String[] getTransactionsInNeedOfRecovery() {
		throw new UnsupportedOperationException("should never be called, because the MicroserviceXAResource handles this internally");
	}

}
