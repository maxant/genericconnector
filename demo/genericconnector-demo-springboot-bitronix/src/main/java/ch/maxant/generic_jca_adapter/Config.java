package ch.maxant.generic_jca_adapter;

import javax.naming.Context;
import javax.naming.NamingException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import bitronix.tm.jndi.BitronixContext;

@Configuration
public class Config {

    @Bean(name="xa/bookingService")
    public BasicTransactionAssistanceFactory bookingSystemFactory() throws NamingException {
        Context ctx = new BitronixContext();
        BasicTransactionAssistanceFactory microserviceFactory = (BasicTransactionAssistanceFactory) ctx.lookup("xa/bookingService");
        return microserviceFactory;
    }
    
    @Bean(name="xa/letterService")
    public BasicTransactionAssistanceFactory letterServiceFactory() throws NamingException {
    	Context ctx = new BitronixContext();
    	BasicTransactionAssistanceFactory microserviceFactory = (BasicTransactionAssistanceFactory) ctx.lookup("xa/letterService");
    	return microserviceFactory;
    }
	
}
