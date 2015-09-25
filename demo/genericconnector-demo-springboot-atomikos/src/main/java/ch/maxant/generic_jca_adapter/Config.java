package ch.maxant.generic_jca_adapter;

import javax.naming.NamingException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public BasicTransactionAssistanceFactory factory() throws NamingException {
        BasicTransactionAssistanceFactory microserviceFactory = new BasicTransactionAssistanceFactoryImpl("xa/ms1");
        return microserviceFactory;
    }
	
}
