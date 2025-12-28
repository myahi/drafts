package fr.lbp.config;

import com.tibco.tibjms.TibjmsConnectionFactory;
import com.tibco.tibjms.TibjmsQueueConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.component.jms.JmsComponent;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

@Slf4j
@Configuration
public class EMSConfiguration {
    
    @Value("${tibco.ems.server-url}")
    private String serverUrl;
    
    @Value("${tibco.ems.username}")
    private String username;
    
    @Value("${tibco.ems.password}")
    private String password;
    
    @Value("${tibco.ems.pool.enabled:true}")
    private boolean poolEnabled;
    
    @Value("${tibco.ems.pool.max-connections:10}")
    private int maxConnections;
    
    @Value("${tibco.ems.reconnect.attempts:3}")
    private int reconnectAttempts;
    
    @Value("${tibco.ems.reconnect.delay:5000}")
    private long reconnectDelay;
    
    /**
     * ConnectionFactory TIBCO EMS de base
     */
    @Bean(name = "tibjmsConnectionFactory")
    public TibjmsConnectionFactory tibjmsConnectionFactory() throws JMSException {
        log.info("Creating TIBCO EMS ConnectionFactory - serverUrl={}", serverUrl);
        
        TibjmsQueueConnectionFactory factory = new TibjmsQueueConnectionFactory();
        
        // Configuration de base
        factory.setServerUrl(serverUrl);
        factory.setUserName(username);
        factory.setUserPassword(password);
        
        // Configuration de reconnexion
        factory.setReconnAttemptCount(reconnectAttempts);
        factory.setReconnAttemptDelay(reconnectDelay);
        factory.setReconnAttemptTimeout(30000);
        
        // Configuration SSL (si activé)
        // factory.setSSLVendor("j2se");
        // factory.setSSLEnableVerifyHost(false);
        
        log.info("TIBCO EMS ConnectionFactory created successfully");
        return factory;
    }
    
    /**
     * ConnectionFactory avec pool (recommandé pour production)
     */
    @Bean(name = "emsConnectionFactory")
    @ConditionalOnProperty(name = "tibco.ems.pool.enabled", havingValue = "true", matchIfMissing = true)
    public ConnectionFactory pooledEmsConnectionFactory(TibjmsConnectionFactory tibjmsConnectionFactory) {
        log.info("Creating pooled EMS ConnectionFactory - maxConnections={}", maxConnections);
        
        JmsPoolConnectionFactory pooledFactory = new JmsPoolConnectionFactory();
        pooledFactory.setConnectionFactory(tibjmsConnectionFactory);
        
        // Configuration du pool
        pooledFactory.setMaxConnections(maxConnections);
        pooledFactory.setMaxSessionsPerConnection(10);
        pooledFactory.setBlockIfSessionPoolIsFull(true);
        pooledFactory.setBlockIfSessionPoolIsFullTimeout(-1);
        pooledFactory.setConnectionIdleTimeout(30000);
        pooledFactory.setUseAnonymousProducers(false);
        
        log.info("Pooled EMS ConnectionFactory created");
        return pooledFactory;
    }
    
    /**
     * ConnectionFactory sans pool (pour DEV/TEST)
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
    
    /**
     * JmsTemplate pour envoyer des messages directement (optionnel)
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory emsConnectionFactory) {
        log.info("Creating JmsTemplate");
        
        JmsTemplate template = new JmsTemplate(emsConnectionFactory);
        
        // Configuration
        template.setDeliveryPersistent(true);
        template.setPriority(4);
        template.setExplicitQosEnabled(true);
        template.setReceiveTimeout(5000);
        
        return template;
    }
    
    /**
     * Composant Camel JMS pour EMS
     */
    @Bean(name = "ems")
    public JmsComponent emsJmsComponent(ConnectionFactory emsConnectionFactory) {
        log.info("Creating Camel JMS component for EMS");
        
        JmsComponent jmsComponent = new JmsComponent();
        jmsComponent.setConnectionFactory(emsConnectionFactory);
        
        // Configuration Camel
        jmsComponent.setRequestTimeout(20000);
        jmsComponent.setConcurrentConsumers(5);
        jmsComponent.setMaxConcurrentConsumers(10);
        jmsComponent.setCacheLevelName("CACHE_CONSUMER");
        jmsComponent.setDeliveryPersistent(true);
        jmsComponent.setExplicitQosEnabled(true);
        
        log.info("Camel JMS component created");
        return jmsComponent;
    }
}
