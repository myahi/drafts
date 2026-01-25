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

    @Bean(name = "mqJndiContextA", destroyMethod = "close")
    public Context mqJndiContextA(@Value("${mq.a.jndi.bindingsDir}") Path bindingsDir) {
        return createContext(bindingsDir, "A");
    }

    @Bean(name = "mqJndiContextB", destroyMethod = "close")
    public Context mqJndiContextB(@Value("${mq.b.jndi.bindingsDir}") Path bindingsDir) {
        return createContext(bindingsDir, "B");
    }

    @Bean(name = "mqConnectionFactoryA")
    public ConnectionFactory mqConnectionFactoryA(
            @Value("${mq.a.jndi.connectionFactory}") String cfJndiName,
            @javax.annotation.Resource(name = "mqJndiContextA") Context ctx
    ) {
        return lookupCf(ctx, cfJndiName, "A");
    }

    @Bean(name = "mqConnectionFactoryB")
    public ConnectionFactory mqConnectionFactoryB(
            @Value("${mq.b.jndi.connectionFactory}") String cfJndiName,
            @javax.annotation.Resource(name = "mqJndiContextB") Context ctx
    ) {
        return lookupCf(ctx, cfJndiName, "B");
    }

    private Context createContext(Path bindingsDir, String label) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.fscontext.RefFSContextFactory");
            env.put(Context.PROVIDER_URL, bindingsDir.toUri().toString());
            return new InitialContext(env);
        } catch (Exception e) {
            throw new MqJndiException("Unable to init JNDI context " + label + " for " + bindingsDir, e);
        }
    }

    private ConnectionFactory lookupCf(Context ctx, String name, String label) {
        try {
            return (ConnectionFactory) ctx.lookup(name);
        } catch (Exception e) {
            throw new MqJndiException("Unable to lookup ConnectionFactory " + label + ": " + name, e);
        }
    }
}
