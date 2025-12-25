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
    
    public AuditBuilder metadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
    
    public AuditBuilder addMetadata(Map<String, Object> additionalMetadata) {
        if (additionalMetadata != null) {
            this.metadata.putAll(additionalMetadata);
        }
        return this;
    }
    
    public AuditBuilder status(String status) {
        this.status = status;
        return this;
    }
    
    public AuditBuilder payload(Object payload) {
        this.payload = payload;
        return this;
    }
    
    public AuditBuilder eventType(String eventType) {
        this.eventType = eventType;
        return this;
    }
    
    /**
     * Construit et envoie l'audit
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

/**
 * Envoie l'événement d'audit à la queue SEDA
 * Cette méthode reçoit un AuditEvent déjà construit
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
    }
}
