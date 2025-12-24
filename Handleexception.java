
org.apache.camel.dataformat.bindy.fixed.BindyFixedLengthDataFormat.unmarshal
    
from("cxf:bean:myEndpoint")
    .routeId("soap-fixed-length-service")
    .streamCaching()  // Important pour relire le body
    
    // Log entrée (propre)
    .log(LoggingLevel.INFO, "📨 Message SOAP reçu (longueur: ${body.length})")
    
    // Gestion globale des erreurs
    .onException(Exception.class)
        .handled(true)
        .maximumRedeliveries(0)
        .logStackTrace(false)  // Pas de stack trace !
        .process(exchange -> {
            Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            String originalBody = exchange.getIn().getBody(String.class);
            
            // Log structuré et concis
            log.error("Erreur traitement message - Type: {}, Message: {}, BodyLength: {}", 
                     ex.getClass().getSimpleName(),
                     ex.getMessage(),
                     originalBody != null ? originalBody.length() : 0);
            
            // Log du body en erreur (optionnel, attention aux données sensibles)
            if (log.isDebugEnabled()) {
                log.debug("Body en erreur: {}", originalBody);
            }
            
            // Créer SOAP Fault propre
            SoapFault fault = new SoapFault(
                "Erreur de traitement du message",
                new QName("http://yournamespace.com", "ProcessingError")
            );
            
            // Message d'erreur utilisateur (sans détails techniques)
            fault.setFaultString("Le message reçu ne peut pas être traité. Veuillez vérifier le format.");
            
            // Détails supplémentaires (optionnel)
            Element detail = fault.getOrCreateDetail();
            detail.setTextContent(String.format("Type d'erreur: %s", ex.getClass().getSimpleName()));
            
            throw fault;
        })
    .end()
    
    // Validation préalable
    .process(exchange -> {
        String body = exchange.getIn().getBody(String.class);
        
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Message vide");
        }
        
        if (body.length() != 150) {  // Exemple: longueur attendue
            throw new IllegalArgumentException(
                String.format("Longueur invalide: attendu 150, reçu %d", body.length())
            );
        }
    })
    
    // Unmarshalling
    .doTry()
        .unmarshal().bindy(BindyType.Fixed, YourFixedLengthClass.class)
        .log(LoggingLevel.INFO, "✅ Message unmarshallé avec succès")
    .doCatch(BindyException.class)
        .process(exchange -> {
            BindyException ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, BindyException.class);
            throw new IllegalArgumentException("Format Bindy invalide: " + ex.getMessage());
        })
    .end()
    
    // Traitement business
    .to("seda:processMessage?waitForTaskToComplete=Never")
    
    // Réponse SOAP
    .transform().constant("OK")
    .log(LoggingLevel.INFO, "✅ Réponse envoyée au client");
