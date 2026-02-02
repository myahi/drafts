package fr.labanquepostale.marches.eai.core.configuration;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.component.jms.JmsComponent;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import com.tibco.tibjms.TibjmsConnectionFactory;

@Slf4j
@Configuration
public class EMSConfiguration {

    @Value("${tibco.ems.server-url}")
    private String serverUrl;

    @Value("${tibco.ems.username}")
    private String username;

    @Value("${tibco.ems.password}")
    private String password;

    @Value("${tibco.ems.pool.max-connections:10}")
    private int maxConnections;

    @Value("${tibco.ems.reconnect.attempts:3}")
    private int reconnectAttempts;

    @Value("${tibco.ems.reconnect.delay:5000}")
    private int reconnectDelay;

    /**
     * ConnectionFactory EMS (Jakarta)
     */
    @Bean(name = "tibjmsConnectionFactory")
    public TibjmsConnectionFactory tibjmsConnectionFactory() throws JMSException {
        log.info("Creating TIBCO EMS ConnectionFactory - serverUrl={}", serverUrl);

        TibjmsConnectionFactory factory = new TibjmsConnectionFactory();
        factory.setServerUrl(serverUrl);
        factory.setUserName(username);
        factory.setUserPassword(password);

        factory.setReconnAttemptCount(reconnectAttempts);
        factory.setReconnAttemptDelay(reconnectDelay);
        factory.setReconnAttemptTimeout(30000);

        log.info("TIBCO EMS ConnectionFactory created successfully");
        return factory;
    }

    /**
     * ConnectionFactory avec pool
     */
    @Bean(name = "emsConnectionFactory")
    @Primary
    @ConditionalOnProperty(name = "tibco.ems.pool.enabled", havingValue = "true", matchIfMissing = true)
    public ConnectionFactory pooledEmsConnectionFactory(TibjmsConnectionFactory tibjmsConnectionFactory) {
        log.info("Creating pooled EMS ConnectionFactory - maxConnections={}", maxConnections);

        JmsPoolConnectionFactory pooledFactory = new JmsPoolConnectionFactory();
        pooledFactory.setConnectionFactory(tibjmsConnectionFactory);

        pooledFactory.setMaxConnections(maxConnections);
        pooledFactory.setMaxSessionsPerConnection(10);

        pooledFactory.setBlockIfSessionPoolIsFull(true);
        pooledFactory.setBlockIfSessionPoolIsFullTimeout(30000); // ⚠️ évite -1 (blocage infini)

        pooledFactory.setConnectionIdleTimeout(300000); // 5 min (à ajuster)
        pooledFactory.setUseAnonymousProducers(false);

        return pooledFactory;
    }

    /**
     * ConnectionFactory sans pool (DEV/TEST)
     */
    @Bean(name = "emsConnectionFactory")
    @ConditionalOnProperty(name = "tibco.ems.pool.enabled", havingValue = "false")
    public ConnectionFactory cachedEmsConnectionFactory(TibjmsConnectionFactory tibjmsConnectionFactory) {
        log.info("Creating cached EMS ConnectionFactory (no pool)");

        CachingConnectionFactory cachingFactory = new CachingConnectionFactory(tibjmsConnectionFactory);
        cachingFactory.setSessionCacheSize(10);
        cachingFactory.setCacheConsumers(false);
        cachingFactory.setCacheProducers(true);

        return cachingFactory;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory emsConnectionFactory) {
        JmsTemplate template = new JmsTemplate(emsConnectionFactory);
        template.setDeliveryPersistent(true);
        template.setExplicitQosEnabled(true);
        template.setPriority(4);
        template.setReceiveTimeout(5000);
        return template;
    }

    @Bean(name = "ems")
    public JmsComponent emsJmsComponent(ConnectionFactory emsConnectionFactory) {
        JmsComponent jmsComponent = new JmsComponent();
        jmsComponent.setConnectionFactory(emsConnectionFactory);

        jmsComponent.setRequestTimeout(20000);
        jmsComponent.setConcurrentConsumers(2);
        jmsComponent.setMaxConcurrentConsumers(5);
        jmsComponent.setCacheLevelName("CACHE_CONSUMER");

        jmsComponent.setDeliveryPersistent(true);
        jmsComponent.setExplicitQosEnabled(true);

        return jmsComponent;
    }
}
