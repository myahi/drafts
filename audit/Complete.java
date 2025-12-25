from("cxf:bean:soapEndpoint")
    .routeId("order-processing-route")
    
    // Initialiser le ProcessContext une fois au début
    .process(exchange -> {
        ProcessContext ctx = new ProcessContext();
        ctx.setProcessId(System.currentTimeMillis());
        ctx.setProjectName("OrderManagement");
        ctx.setEngineName("order-processing-route");
        ctx.setRestartedFromCheckpoint(false);
        ctx.setCustomId("EXEC-" + System.currentTimeMillis());
        
        // Ajouter des trackingInfo
        ctx.getTrackingInfo().add("userId:" + exchange.getIn().getHeader("userId"));
        ctx.getTrackingInfo().add("source:SOAP");
        
        // Stocker dans l'exchange
        exchange.setProperty("processContext", ctx);
        exchange.setProperty("startTime", System.currentTimeMillis());
    })
    
    // Maintenant tous les audits utiliseront ce ProcessContext
    .process(exchange -> {
        auditHelper.audit(exchange)
            .desc("Réception message")
            .status("RECEIVED")
            .data(exchange.getIn().getBody(String.class))
            .send();
    })
    
    // ... reste de la route
