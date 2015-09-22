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
