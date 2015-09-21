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

import java.io.Closeable;
import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWriter;

@SpringBootApplication
public class Main {

    public static void main(String[] args) throws Exception {
    	setupCommitRollbackHandlerForMicroservice();

    	ApplicationContext context = SpringApplication.run(Main.class, args);
		
    	AppService appService = context.getBean(AppService.class);
		appService.doSomethingWithAGlobalTransactionAndARemoteMicroservice("john");

		Thread.sleep(100); //why does sample from springboot do this?
		((Closeable) context).close();

		//shutdown
		BitronixTransactionConfigurator.unregisterMicroserviceResourceFactory("xa/ms1");
    }

	private static void setupCommitRollbackHandlerForMicroservice() {
		final LetterWriter service = new LetterWebServiceService().getLetterWriterPort();
    	{//setup microservice that we want to call within a transaction
    		CommitRollbackHandler commitRollbackCallback = new CommitRollbackHandler() {
    			@Override
    			public void rollback(String txid) throws Exception {
    				//compensate by cancelling the letter
    				service.cancelLetter(txid);
    			}
    			@Override
    			public void commit(String txid) throws Exception {
    				//nothing to do, this service autocommits.
    			}
    		};
    		BitronixTransactionConfigurator.setup("xa/ms1", commitRollbackCallback, 30000L, new File("."));
    	}
	}

}
