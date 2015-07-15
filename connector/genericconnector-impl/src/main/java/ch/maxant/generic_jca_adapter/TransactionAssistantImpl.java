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

public class TransactionAssistantImpl implements TransactionAssistant {

    /** this implementation of hte TransactionAsssistant simply 
     *  delegates to this managed transaction assistance instance */
    private ManagedTransactionAssistance mc;

    public TransactionAssistantImpl(ManagedTransactionAssistance mc) {
        this.mc = mc;
    }
    
    @Override
    public <O> O executeInActiveTransaction(ExecuteCallback<O> tc) throws Exception {
        return mc.execute(tc);
    }
    
    @Override
    public void close() {
        mc.close(this);
    }

}
