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

based on https://raw.githubusercontent.com/spring-projects/spring-boot/master/spring-boot-samples/spring-boot-sample-jta-bitronix/src/main/java/sample/bitronix/AccountService.java
 */

package ch.maxant.generic_jca_adapter;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWriter;

@Service
@Transactional
public class AppService {

	@Autowired
	private AppRepository appRepository;

	@Autowired
	BasicTransactionAssistanceFactory microserviceFactory;

	public String doSomethingWithAGlobalTransactionAndARemoteMicroservice(String username) throws Exception {

		{//write to local database
			Account acct = new Account();
			acct.setName(username);
			acct.setId(5000);
			this.appRepository.save(acct);
		}

        String msResponse = null;
        try(TransactionAssistant transactionAssistant = microserviceFactory.getTransactionAssistant()){
        	msResponse = transactionAssistant.executeInActiveTransaction(txid->{
        		final LetterWriter service = new LetterWebServiceService().getLetterWriterPort();
        		return service.writeLetter(txid, username);
        	});
        }

        //simulate exception to cause rollback
		if ("john".equals(username)) {
			throw new RuntimeException("Simulated error");
		}

        return msResponse;
	}

}