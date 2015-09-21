package ch.maxant.generic_jca_adapter;

import javax.naming.Context;
import javax.naming.NamingException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import bitronix.tm.jndi.BitronixContext;

@Configuration
public class Config {

    @Bean(destroyMethod = "close")
    public BasicTransactionAssistanceFactory factory() throws NamingException {
        Context ctx = new BitronixContext();
        BasicTransactionAssistanceFactory microserviceFactory = (BasicTransactionAssistanceFactory) ctx.lookup("xa/ms1");
        return microserviceFactory;
    }
	
}
