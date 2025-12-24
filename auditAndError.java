@Override
public void configure() {
    
    // === GESTION DES EXCEPTIONS ===
    onException(Exception.class)
        .maximumRedeliveries(3)
        .redeliveryDelay(1000)
        .wireTap("seda:error-audit")  // Audit asynchrone
        .handled(true)
        .process(exchange -> {
            exchange.getIn().setBody("Erreur technique, réessayez plus tard");
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        });
    
    onException(BindyException.class)
        .maximumRedeliveries(0)
        .wireTap("seda:error-audit")
        .handled(true)
        .process(exchange -> {
            exchange.getIn().setBody("Format de fichier invalide");
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
        });
    
    // === ROUTES MÉTIER ===
    from("direct:processFile")
        .routeId("process-file")
        .wireTap("seda:audit")  // Audit normal
        .unmarshal(bindyDataFormat)
        .to("bean:fileProcessor")
        .wireTap("seda:success-audit");  // Audit succès
    
    // === AUDIT NORMAL (SUCCESS) ===
    from("seda:audit?concurrentConsumers=3")
        .routeId("audit-route")
        .process(exchange -> {
            Map<String, Object> audit = new HashMap<>();
            audit.put("timestamp", Instant.now());
            audit.put("routeId", exchange.getFromRouteId());
            audit.put("status", "PROCESSING");
            exchange.getIn().setBody(audit);
        })
        .marshal().json()
        .to("kafka:audit-topic");
    
    // === AUDIT SUCCESS ===
    from("seda:success-audit")
        .routeId("success-audit-route")
        .process(exchange -> {
            Map<String, Object> audit = new HashMap<>();
            audit.put("timestamp", Instant.now());
            audit.put("routeId", exchange.getFromRouteId());
            audit.put("status", "SUCCESS");
            exchange.getIn().setBody(audit);
        })
        .marshal().json()
        .to("kafka:audit-topic");
    
    // === AUDIT ERREURS ===
    from("seda:error-audit?concurrentConsumers=5")
        .routeId("error-audit-route")
        .process(exchange -> {
            Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            
            Map<String, Object> errorAudit = new HashMap<>();
            errorAudit.put("timestamp", Instant.now());
            errorAudit.put("routeId", exchange.getFromRouteId());
            errorAudit.put("status", "ERROR");
            errorAudit.put("exceptionType", exception.getClass().getSimpleName());
            errorAudit.put("exceptionMessage", exception.getMessage());
            errorAudit.put("correlationId", exchange.getIn().getHeader("correlationId"));
            
            // Stack trace limitée (premières lignes)
            String stackTrace = getStackTraceAsString(exception);
            errorAudit.put("stackTrace", stackTrace.substring(0, Math.min(1000, stackTrace.length())));
            
            exchange.getIn().setBody(errorAudit);
        })
        .marshal().json()
        .multicast()
            .to("kafka:error-topic")
            .to("file:errors?fileName=error-${date:now:yyyyMMdd-HHmmss}.json")
            .to("log:error-audit?level=ERROR&showException=false");  // Pas de stack dans les logs
}
