package com.mycompany.app.camel;

import org.apache.camel.component.jms.JmsComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;

@Configuration
public class JmsCamelConfig {

    @Bean(name = "mqJms")
    public JmsComponent mqJms(ConnectionFactory mqConnectionFactory) {
        // Auto-ack (le plus simple)
        return JmsComponent.jmsComponentAutoAcknowledge(mqConnectionFactory);

        // Si tu veux des transactions JMS plus tard :
        // return JmsComponent.jmsComponentTransacted(mqConnectionFactory);
    }
}
