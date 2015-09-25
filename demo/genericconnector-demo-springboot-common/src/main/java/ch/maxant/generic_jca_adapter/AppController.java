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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * a rest controller which calls thru to a service running inside a transaction
 */
@RestController
public class AppController {

	@Autowired
	AppService appService;

    @RequestMapping("/createUser")
    public String createUser(@RequestParam(value="username") String username) throws Exception {
    	return appService.doSomethingWithAGlobalTransactionAndARemoteMicroservice(username);
    }
	
}
