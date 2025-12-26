package fr.lbp.routes;

import fr.lbp.lib.audit.TibcoAuditHelper;
import fr.lbp.lib.error.TibcoErrorHelper;
import fr.lbp.lib.model.enginetypes.ProcessContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ErrorHandlingExamples extends RouteBuilder {
    
    @Autowired
    private TibcoAuditHelper auditHelper;
    
    @Autowired
    private TibcoErrorHelper errorHelper;
    
    @Override
    public void configure() {
        
        // ============================================================
        // EXEMPLE 1: Gestion d'erreur simple avec exception
        // ============================================================
        from("direct:example1-simple-error")
            .routeId("example1-error-route")
            
            .process(exchange -> {
                exchange.setProperty("startTime", System.currentTimeMillis());
            })
            
            .to("direct:processing")
            
            // Gestion d'erreur globale
            .onException(ValidationException.class)
                .process(exchange -> {
                    ValidationException ex = (ValidationException) exchange.getProperty(
                        Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // Créer l'erreur à partir de l'exception
                    errorHelper.error(exchange, ex)
                        .msgCode("VAL-001")
                        .meta("errorType", "VALIDATION")
                        .meta("severity", "HIGH")
                        .send();
                })
                .handled(true)
            .end();
        
        // ============================================================
        // EXEMPLE 2: Erreur avec données et références
        // ============================================================
        from("direct:example2-detailed-error")
            .routeId("example2-error-route")
            
            .onException(Exception.class)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(
                        Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    Order order = exchange.getIn().getBody(Order.class);
                    
                    errorHelper.error(exchange, exception)
                        .msgCode("ORD-500")
                        .data(order)  // Données principales
                        .ref("orderId", order.getId())
                        .ref("customerId", order.getCustomerId())
                        .meta("orderType", order.getType())
                        .meta("amount", order.getAmount())
                        .meta("errorType", "TECHNICAL")
                        .meta("severity", "CRITICAL")
                        .send();
                })
                .handled(true)
            .end();
        
        // ============================================================
        // EXEMPLE 3: Utiliser errorFromExchange (plus simple)
        // ============================================================
        from("direct:example3-from-exchange")
            .routeId("example3-error-route")
            
            .onException(Exception.class)
                .process(exchange -> {
                    // Récupère automatiquement l'exception de l'exchange
                    errorHelper.errorFromExchange(exchange)
                        .msgCode("GEN-ERROR")
                        .meta("context", "order-processing")
                        .send();
                })
                .handled(true)
            .end();
        
        // ============================================================
        // EXEMPLE 4: Erreur avec ProcessContext personnalisé
        // ============================================================
        from("direct:example4-custom-context")
            .routeId("example4-error-route")
            
            .process(exchange -> {
                // Init ProcessContext personnalisé
                ProcessContext ctx = new ProcessContext();
                ctx.setProcessId(System.currentTimeMillis());
                ctx.setProjectName("PaymentProcessing");
                ctx.setEngineName("payment-validator");
                ctx.setRestartedFromCheckpoint(false);
                ctx.setCustomId("PAY-" + System.currentTimeMillis());
                ctx.getTrackingInfo().add("paymentId:12345");
                
                exchange.setProperty("processContext", ctx);
            })
            
            .onException(PaymentException.class)
                .process(exchange -> {
                    PaymentException ex = (PaymentException) exchange.getProperty(
                        Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    errorHelper.error(exchange, ex)
                        .msgCode("PAY-001")
                        .ref("paymentId", ex.getPaymentId())
                        .ref("transactionId", ex.getTransactionId())
                        .meta("paymentMethod", ex.getPaymentMethod())
                        .meta("errorType", "PAYMENT_DECLINED")
                        .send();
                })
                .handled(true)
            .end();
        
        // ============================================================
        // EXEMPLE 5: Erreur manuelle (sans exception Java)
        // ============================================================
        from("direct:example5-manual-error")
            .routeId("example5-error-route")
            
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // Validation métier
                if (order.getAmount() > 10000) {
                    // Créer une erreur sans exception
                    errorHelper.error(exchange)
                        .msg("Montant trop élevé pour validation automatique")
                        .fullClass("fr.lbp.validation.BusinessRuleException")
                        .clazz("BusinessRuleException")
                        .processStack(exchange.getFromRouteId())
                        .stackTrace("Business validation failed at: " + 
                            exchange.getFromRouteId())
                        .msgCode("BUS-MAX-AMOUNT")
                        .data(order)
                        .ref("orderId", order.getId())
                        .meta("maxAmount", "10000")
                        .meta("actualAmount", order.getAmount())
                        .meta("validationType", "BUSINESS")
                        .send();
                    
                    // Arrêter le traitement
                    exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
                }
            });
        
        // ============================================================
        // EXEMPLE 6: Erreur avec tracking et métadonnées riches
        // ============================================================
        from("direct:example6-rich-metadata")
            .routeId("example6-error-route")
            
            .onException(Exception.class)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(
                        Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    errorHelper.error(exchange, exception)
                        .msgCode("SYS-ERROR")
                        .tracking("error:occurred")
                        .tracking("timestamp:" + System.currentTimeMillis())
                        .meta("userId", exchange.getIn().getHeader("userId"))
                        .meta("clientIp", exchange.getIn().getHeader("X-Forwarded-For"))
                        .meta("userAgent", exchange.getIn().getHeader("User-Agent"))
                        .meta("endpoint", exchange.getFromEndpoint().getEndpointUri())
                        .meta("messageSize", exchange.getIn().getBody(String.class).length())
                        .metas(Map.of(
                            "jvmMemory", Runtime.getRuntime().freeMemory(),
                            "threadCount", Thread.activeCount(),
                            "timestamp", System.currentTimeMillis()
                        ))
                        .send();
                })
                .handled(true)
            .end();
        
        // ============================================================
        // EXEMPLE 7: Route complète avec audit + erreur
        // ============================================================
        from("cxf:bean:soapEndpoint")
            .routeId("complete-error-handling-route")
            
            // Init
            .process(exchange -> {
                ProcessContext ctx = new ProcessContext();
                ctx.setProcessId(System.currentTimeMillis());
                ctx.setProjectName("OrderManagement");
                ctx.setEngineName("order-processing");
                ctx.setRestartedFromCheckpoint(false);
                ctx.setCustomId("EXEC-" + System.currentTimeMillis());
                
                exchange.setProperty("processContext", ctx);
                exchange.setProperty("startTime", System.currentTimeMillis());
            })
            
            // Audit début
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Début du traitement")
                    .status("STARTED")
                    .data(exchange.getIn().getBody(String.class))
                    .send();
            })
            
            .unmarshal().jaxb()
            
            // Traitement
            .to("bean:orderService?method=process")
            
            // Audit succès
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Traitement réussi")
                    .status("SUCCESS")
                    .data(exchange.getIn().getBody())
                    .send();
            })
            
            .marshal().jaxb()
            
            // Gestion des erreurs métier
            .onException(ValidationException.class)
                .process(exchange -> {
                    ValidationException ex = (ValidationException) exchange.getProperty(
                        Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // Audit d'erreur
                    auditHelper.audit(exchange)
                        .desc("Erreur de validation")
                        .status("VALIDATION_ERROR")
                        .data(ex.getMessage())
                        .meta("errorType", "BUSINESS")
                        .send();
                    
                    // Erreur détaillée
                    errorHelper.error(exchange, ex)
                        .msgCode("VAL-001")
                        .data(exchange.getIn().getBody())
                        .ref("validationRule", ex.getRuleName())
                        .meta("errorType", "BUSINESS")
                        .meta("severity", "MEDIUM")
                        .send();
                })
                .handled(true)
                .setBody(constant("Validation failed"))
            .end()
            
            // Gestion des erreurs techniques
            .onException(Exception.class)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(
                        Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // Audit d'erreur
                    auditHelper.audit(exchange)
                        .desc("Erreur technique")
                        .status("ERROR")
                        .data(exception.getMessage())
                        .meta("errorType", "TECHNICAL")
                        .send();
                    
                    // Erreur détaillée
                    errorHelper.error(exchange, exception)
                        .msgCode("TECH-500")
                        .meta("errorType", "TECHNICAL")
                        .meta("severity", "CRITICAL")
                        .tracking("error:critical")
                        .send();
                })
                .handled(true)
                .setBody(constant("Technical error"))
            .end();
        
        // ============================================================
        // Route de traitement des erreurs
        // ============================================================
        from("seda:error?concurrentConsumers=5")
            .routeId("error-processor")
            .log("Error received: ${body.errorReport.msg}")
            // Marshaller en XML
            .marshal().jaxb()
            .log("Error XML: ${body}")
            // Envoyer vers votre système d'erreur
            .to("file:///opt/errors?fileName=error-${date:now:yyyyMMdd-HHmmss}.xml")
            // Ou vers JMS
            // .to("jms:queue:ERROR.QUEUE")
            // Ou vers une base de données
            // .to("jpa:ErrorEntity")
            ;
    }
}
