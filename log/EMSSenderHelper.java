package fr.lbp.lib.ems;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EMSSenderHelper {
    
    @Autowired
    private ProducerTemplate producerTemplate;
    
    @Autowired
    private JmsTemplate jmsTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${tibco.ems.queues.audit}")
    private String auditQueue;
    
    @Value("${tibco.ems.queues.error}")
    private String errorQueue;
    
    @Value("${tibco.ems.message.delivery-mode:PERSISTENT}")
    private String deliveryMode;
    
    @Value("${tibco.ems.message.priority:4}")
    private int priority;
    
    @Value("${tibco.ems.message.time-to-live:0}")
    private long timeToLive;
    
    /**
     * Envoie un message vers une queue EMS (via Camel)
     */
    public void sendToQueue(String queueName, Object message) {
        sendToQueue(queueName, message, null);
    }
    
    /**
     * Envoie un message avec headers vers une queue EMS
     */
    public void sendToQueue(String queueName, Object message, Map<String, Object> headers) {
        try {
            String endpoint = "ems:queue:" + queueName;
            
            if (headers != null && !headers.isEmpty()) {
                producerTemplate.sendBodyAndHeaders(endpoint, message, headers);
            } else {
                producerTemplate.sendBody(endpoint, message);
            }
            
            log.debug("Message sent to EMS queue: {} - messageType={}", 
                queueName, message.getClass().getSimpleName());
            
        } catch (Exception e) {
            log.error("Failed to send message to EMS queue: {}", queueName, e);
            throw new EMSException("Failed to send message to queue: " + queueName, e);
        }
    }
    
    /**
     * Envoie un message de manière asynchrone
     */
    public void sendToQueueAsync(String queueName, Object message) {
        sendToQueueAsync(queueName, message, null);
    }
    
    /**
     * Envoie un message de manière asynchrone avec headers
     */
    public void sendToQueueAsync(String queueName, Object message, Map<String, Object> headers) {
        try {
            String endpoint = "ems:queue:" + queueName;
            
            if (headers != null && !headers.isEmpty()) {
                producerTemplate.asyncSendBodyAndHeaders(endpoint, message, headers);
            } else {
                producerTemplate.asyncSendBody(endpoint, message);
            }
            
            log.debug("Message sent asynchronously to EMS queue: {}", queueName);
            
        } catch (Exception e) {
            log.error("Failed to send async message to EMS queue: {}", queueName, e);
            throw new EMSException("Failed to send async message to queue: " + queueName, e);
        }
    }
    
    /**
     * Envoie un message XML vers une queue
     */
    public void sendXmlToQueue(String queueName, String xmlContent) {
        sendXmlToQueue(queueName, xmlContent, null);
    }
    
    /**
     * Envoie un message XML avec headers
     */
    public void sendXmlToQueue(String queueName, String xmlContent, Map<String, Object> headers) {
        Map<String, Object> allHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
        allHeaders.put("ContentType", "text/xml");
        allHeaders.put("JMSType", "TextMessage");
        
        sendToQueue(queueName, xmlContent, allHeaders);
    }
    
    /**
     * Envoie un objet sérialisé en JSON vers une queue
     */
    public void sendJsonToQueue(String queueName, Object object) {
        sendJsonToQueue(queueName, object, null);
    }
    
    /**
     * Envoie un objet sérialisé en JSON avec headers
     */
    public void sendJsonToQueue(String queueName, Object object, Map<String, Object> headers) {
        try {
            String json = objectMapper.writeValueAsString(object);
            
            Map<String, Object> allHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
            allHeaders.put("ContentType", "application/json");
            allHeaders.put("JMSType", "TextMessage");
            
            sendToQueue(queueName, json, allHeaders);
            
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON", e);
            throw new EMSException("Failed to serialize object to JSON", e);
        }
    }
    
    /**
     * Envoie vers la queue d'audit
     */
    public void sendAudit(Object auditMessage) {
        sendToQueueAsync(auditQueue, auditMessage);
    }
    
    /**
     * Envoie vers la queue d'erreur
     */
    public void sendError(Object errorMessage) {
        sendToQueue(errorQueue, errorMessage);
    }
    
    /**
     * Builder pour envoyer des messages avec options avancées
     */
    public EMSMessageBuilder message(Object body) {
        return new EMSMessageBuilder(body, this);
    }
    
    /**
     * Builder fluent pour envoyer des messages EMS
     */
    public static class EMSMessageBuilder {
        private final Object body;
        private final EMSSenderHelper helper;
        private final Map<String, Object> headers = new HashMap<>();
        private String queueName;
        private boolean async = false;
        
        EMSMessageBuilder(Object body, EMSSenderHelper helper) {
            this.body = body;
            this.helper = helper;
        }
        
        public EMSMessageBuilder toQueue(String queueName) {
            this.queueName = queueName;
            return this;
        }
        
        public EMSMessageBuilder header(String key, Object value) {
            this.headers.put(key, value);
            return this;
        }
        
        public EMSMessageBuilder headers(Map<String, Object> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }
        
        public EMSMessageBuilder correlationId(String correlationId) {
            this.headers.put("JMSCorrelationID", correlationId);
            return this;
        }
        
        public EMSMessageBuilder priority(int priority) {
            this.headers.put("JMSPriority", priority);
            return this;
        }
        
        public EMSMessageBuilder replyTo(String replyToQueue) {
            this.headers.put("JMSReplyTo", replyToQueue);
            return this;
        }
        
        public EMSMessageBuilder async() {
            this.async = true;
            return this;
        }
        
        public void send() {
            if (queueName == null) {
                throw new IllegalStateException("Queue name must be specified with .toQueue()");
            }
            
            if (async) {
                helper.sendToQueueAsync(queueName, body, headers);
            } else {
                helper.sendToQueue(queueName, body, headers);
            }
        }
    }
}
