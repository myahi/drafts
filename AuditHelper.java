package com.mycompany.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AuditHelper {
    
    @Autowired
    private ProducerTemplate producerTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Audite le début d'un traitement
     */
    public void auditStart(Exchange exchange, Object payload) {
        AuditEvent event = AuditEvent.builder()
            .eventType("START")
            .userId(exchange.getIn().getHeader("userId", String.class))
            .correlationId(exchange.getExchangeId())
            .timestamp(Instant.now())
            .endpoint(exchange.getFromEndpoint() != null ? 
                exchange.getFromEndpoint().getEndpointUri() : null)
            .payload(payload)
            .status("INITIATED")
            .build();
        
        sendAudit(event);
    }
    
    /**
     * Audite la fin d'un traitement
     */
    public void auditEnd(Exchange exchange, Object payload) {
        Long startTime = exchange.getProperty("startTime", Long.class);
        Long duration = startTime != null ? 
            System.currentTimeMillis() - startTime : null;
        
        AuditEvent event = AuditEvent.builder()
            .eventType("END")
            .userId(exchange.getIn().getHeader("userId", String.class))
            .correlationId(exchange.getExchangeId())
            .timestamp(Instant.now())
            .endpoint(exchange.getFromEndpoint() != null ? 
                exchange.getFromEndpoint().getEndpointUri() : null)
            .payload(payload)
            .duration(duration)
            .status("COMPLETED")
            .build();
        
        sendAudit(event);
    }
    
    /**
     * Audite une erreur
     */
    public void auditError(Exchange exchange, Exception error) {
        Long startTime = exchange.getProperty("startTime", Long.class);
        Long duration = startTime != null ? 
            System.currentTimeMillis() - startTime : null;
        
        Map<String, Object> errorMetadata = new HashMap<>();
        errorMetadata.put("errorMessage", error.getMessage());
        errorMetadata.put("errorClass", error.getClass().getName());
        
        if (error.getCause() != null) {
            errorMetadata.put("causeMessage", error.getCause().getMessage());
            errorMetadata.put("causeClass", error.getCause().getClass().getName());
        }
        
        AuditEvent event = AuditEvent.builder()
            .eventType("ERROR")
            .userId(exchange.getIn().getHeader("userId", String.class))
            .correlationId(exchange.getExchangeId())
            .timestamp(Instant.now())
            .endpoint(exchange.getFromEndpoint() != null ? 
                exchange.getFromEndpoint().getEndpointUri() : null)
            .status("FAILED")
            .duration(duration)
            .metadata(errorMetadata)
            .build();
        
        sendAudit(event);
    }
    
    /**
     * Audite un événement personnalisé simple
     */
    public void audit(Exchange exchange, String eventType, String status) {
        audit(exchange, eventType, status, null, null);
    }
    
    /**
     * Audite un événement avec payload
     */
    public void audit(Exchange exchange, String eventType, String status, Object payload) {
        audit(exchange, eventType, status, payload, null);
    }
    
    /**
     * Audite un événement avec métadonnées
     */
    public void audit(Exchange exchange, String eventType, String status, 
                     Map<String, Object> metadata) {
        audit(exchange, eventType, status, null, metadata);
    }
    
    /**
     * Audite un événement complet avec payload et métadonnées
     */
    public void audit(Exchange exchange, String eventType, String status, 
                     Object payload, Map<String, Object> metadata) {
        Long startTime = exchange.getProperty("startTime", Long.class);
        Long duration = startTime != null ? 
            System.currentTimeMillis() - startTime : null;
        
        AuditEvent event = AuditEvent.builder()
            .eventType(eventType)
            .userId(exchange.getIn().getHeader("userId", String.class))
            .correlationId(exchange.getExchangeId())
            .timestamp(Instant.now())
            .endpoint(exchange.getFromEndpoint() != null ? 
                exchange.getFromEndpoint().getEndpointUri() : null)
            .payload(payload)
            .status(status)
            .duration(duration)
            .metadata(metadata)
            .build();
        
        sendAudit(event);
    }
    
    /**
     * Crée un builder pour un audit avec métadonnées dynamiques
     */
    public AuditBuilder createAudit(Exchange exchange, String eventType) {
        return new AuditBuilder(exchange, eventType, this);
    }
    
    /**
     * Envoie l'événement d'audit à la queue SEDA
     */
    void sendAudit(AuditEvent event) {
        try {
            String auditJson = objectMapper.writeValueAsString(event);
            producerTemplate.asyncSendBody("seda:audit", auditJson);
            log.debug("Audit event sent: type={}, correlationId={}", 
                event.getEventType(), event.getCorrelationId());
        } catch (Exception e) {
            log.error("Failed to send audit event: type={}, correlationId={}", 
                event.getEventType(), event.getCorrelationId(), e);
            // Ne pas propager l'erreur pour ne pas casser le flux principal
        }
    }
    
    /**
     * Builder fluent pour créer des audits avec métadonnées dynamiques
     */
    public static class AuditBuilder {
        private final Exchange exchange;
        private final AuditHelper helper;
        private final Map<String, Object> metadata = new HashMap<>();
        private String eventType;
        private String status;
        private Object payload;
        
        AuditBuilder(Exchange exchange, String eventType, AuditHelper helper) {
            this.exchange = exchange;
            this.eventType = eventType;
            this.helper = helper;
        }
        
        /**
         * Ajoute une métadonnée
         */
        public AuditBuilder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        /**
         * Ajoute plusieurs métadonnées
         */
        public AuditBuilder addMetadata(Map<String, Object> additionalMetadata) {
            if (additionalMetadata != null) {
                this.metadata.putAll(additionalMetadata);
            }
            return this;
        }
        
        /**
         * Définit le statut
         */
        public AuditBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        /**
         * Définit le payload
         */
        public AuditBuilder payload(Object payload) {
            this.payload = payload;
            return this;
        }
        
        /**
         * Définit le type d'événement
         */
        public AuditBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        /**
         * Envoie l'audit
         */
        public void send() {
            Long startTime = exchange.getProperty("startTime", Long.class);
            Long duration = startTime != null ? 
                System.currentTimeMillis() - startTime : null;
            
            AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .userId(exchange.getIn().getHeader("userId", String.class))
                .correlationId(exchange.getExchangeId())
                .timestamp(Instant.now())
                .endpoint(exchange.getFromEndpoint() != null ? 
                    exchange.getFromEndpoint().getEndpointUri() : null)
                .status(status)
                .payload(payload)
                .duration(duration)
                .metadata(metadata.isEmpty() ? null : metadata)
                .build();
            
            helper.sendAudit(event);
        }
    }
}
