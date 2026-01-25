package com.mycompany.app.camel;

import org.apache.camel.component.jms.JmsComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;

@Configuration
public class JmsCamelConfig {

    @Bean(name = "mqA")
    public JmsComponent mqA(@javax.annotation.Resource(name = "mqConnectionFactoryA") ConnectionFactory cf) {
        return JmsComponent.jmsComponentAutoAcknowledge(cf);
    }

    @Bean(name = "mqB")
    public JmsComponent mqB(@javax.annotation.Resource(name = "mqConnectionFactoryB") ConnectionFactory cf) {
        return JmsComponent.jmsComponentAutoAcknowledge(cf);
    }
}
