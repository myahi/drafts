package com.mycompany.app.mq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.nio.file.Path;
import java.util.Hashtable;

@Configuration
public class MqJndiConfig {

    @Bean(destroyMethod = "close")
    public Context mqJndiContext(
            @Value("${mq.jndi.bindingsDir}") Path bindingsDir
    ) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.jndi.fscontext.RefFSContextFactory");
            env.put(Context.PROVIDER_URL,
                    bindingsDir.toUri().toString()); // ex: file:/opt/mq/jndi/

            return new InitialContext(env);
        } catch (Exception e) {
            throw new MqJndiException(
                "Unable to initialize JNDI context for bindingsDir=" + bindingsDir, e
            );
        }
    }

    @Bean
    public ConnectionFactory mqConnectionFactory(
            Context mqJndiContext,
            @Value("${mq.jndi.connectionFactory}") String cfJndiName
    ) {
        try {
            return (ConnectionFactory) mqJndiContext.lookup(cfJndiName);
        } catch (Exception e) {
            throw new MqJndiException(
                "Unable to lookup ConnectionFactory: " + cfJndiName, e
            );
        }
    }
}
