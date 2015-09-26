package ch.maxant.generic_jca_adapter;

import javax.naming.NamingException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean(name="xa/bookingService")
    public BasicTransactionAssistanceFactory bookingServiceFactory() throws NamingException {
        BasicTransactionAssistanceFactory microserviceFactory = new BasicTransactionAssistanceFactoryImpl("xa/bookingService");
        return microserviceFactory;
    }
    
    @Bean(name="xa/letterService")
    public BasicTransactionAssistanceFactory letterServiceFactory() throws NamingException {
    	BasicTransactionAssistanceFactory microserviceFactory = new BasicTransactionAssistanceFactoryImpl("xa/letterService");
    	return microserviceFactory;
    }
	
}
