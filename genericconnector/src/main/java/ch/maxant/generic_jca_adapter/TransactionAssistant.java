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


/** 
 * An interface capable of binding something into a transaction.
 * (The factory creates these.) */
public interface TransactionAssistant extends AutoCloseable {

    /** Submit some work (a function) to be bound into the
     * currently active transaction. */
    <O> O executeInTransaction(ExecuteCallback<O> tc) throws Exception;

    /** Call before completing the transaction in order 
     * to free up resources used by the app server. */
    @Override
    void close();
}