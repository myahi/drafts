package fr.lbp.routes;

import fr.lbp.lib.audit.TibcoAuditHelper;
import fr.lbp.lib.model.enginetypes.ProcessContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessContextExamples extends RouteBuilder {
    
    @Autowired
    private TibcoAuditHelper auditHelper;
    
    @Override
    public void configure() {
        
        // ============================================================
        // EXEMPLE 1: ProcessContext automatique (par défaut)
        // ============================================================
        from("direct:example1-auto")
            .routeId("example1-route")
            
            .process(exchange -> {
                // Le ProcessContext est créé automatiquement
                // avec les valeurs par défaut du TibcoAuditHelper
                auditHelper.audit(exchange)
                    .desc("Message traité")
                    .status("SUCCESS")
                    .data("OK")
                    .send();
                // ProcessContext aura:
                // - projectName: nom du CamelContext
                // - engineName: "example1-route"
                // - customId: généré automatiquement
            });
        
        // ============================================================
        // EXEMPLE 2: Initialiser le ProcessContext au début
        // ============================================================
        from("direct:example2-init")
            .routeId("example2-route")
            
            // Initialiser au début de la route
            .process(exchange -> {
                ProcessContext ctx = auditHelper.createProcessContext(
                    "OrderManagement",
                    "order-processor-v2"
                );
                ctx.setCustomId("ORDER-" + System.currentTimeMillis());
                ctx.getTrackingInfo().add("userId:" + exchange.getIn().getHeader("userId"));
                ctx.getTrackingInfo().add("source:REST");
                
                // Stocker dans l'exchange
                auditHelper.initProcessContext(exchange, ctx);
                exchange.setProperty("startTime", System.currentTimeMillis());
            })
            
            // Tous les audits suivants utiliseront ce ProcessContext
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Validation début")
                    .status("VALIDATING")
                    .data("Validation en cours")
                    .send();
            })
            
            .to("direct:validation")
            
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Validation terminée")
                    .status("VALIDATED")
                    .data("OK")
                    .send();
            });
        
        // ============================================================
        // EXEMPLE 3: Enrichir le ProcessContext à chaque étape
        // ============================================================
        from("direct:example3-enrich")
            .routeId("example3-route")
            
            // Init
            .process(exchange -> {
                ProcessContext ctx = new ProcessContext();
                ctx.setProcessId(System.currentTimeMillis());
                ctx.setProjectName("PaymentProcessing");
                ctx.setEngineName("payment-flow");
                ctx.setRestartedFromCheckpoint(false);
                ctx.setCustomId("PAY-" + System.currentTimeMillis());
                
                auditHelper.initProcessContext(exchange, ctx);
            })
            
            // Étape 1: Ajouter info de tracking
            .process(exchange -> {
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                ctx.getTrackingInfo().add("step:fraud-check");
                ctx.getTrackingInfo().add("timestamp:" + System.currentTimeMillis());
                
                auditHelper.audit(exchange)
                    .desc("Contrôle fraude")
                    .status("CHECKING")
                    .data("Analyse en cours")
                    .send();
            })
            
            .to("direct:fraudCheck")
            
            // Étape 2: Enrichir avec paymentId
            .process(exchange -> {
                String paymentId = exchange.getIn().getHeader("paymentId", String.class);
                
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                ctx.getTrackingInfo().add("paymentId:" + paymentId);
                ctx.getTrackingInfo().add("step:authorization");
                
                auditHelper.audit(exchange)
                    .desc("Autorisation paiement")
                    .status("AUTHORIZING")
                    .data(paymentId)
                    .send();
            });
        
        // ============================================================
        // EXEMPLE 4: ProcessContext personnalisé pour un audit spécifique
        // ============================================================
        from("direct:example4-custom")
            .routeId("example4-route")
            
            .process(exchange -> {
                // Audit normal avec ProcessContext par défaut
                auditHelper.audit(exchange)
                    .desc("Traitement normal")
                    .status("PROCESSING")
                    .data("Data")
                    .send();
            })
            
            .process(exchange -> {
                // Créer un ProcessContext spécifique pour cet audit
                ProcessContext specialCtx = new ProcessContext();
                specialCtx.setProcessId(99999L);
                specialCtx.setProjectName("SpecialAudit");
                specialCtx.setEngineName("special-engine");
                specialCtx.setRestartedFromCheckpoint(true);
                specialCtx.setCustomId("SPECIAL-123");
                specialCtx.getTrackingInfo().add("special:true");
                specialCtx.getTrackingInfo().add("priority:HIGH");
                
                // Utiliser le ProcessContext personnalisé pour cet audit uniquement
                auditHelper.audit(exchange)
                    .desc("Audit spécial avec ProcessContext custom")
                    .status("SPECIAL")
                    .data("Special data")
                    .processContext(specialCtx)  // ✅ ProcessContext spécifique
                    .send();
            })
            
            .process(exchange -> {
                // Audit suivant reprend le ProcessContext par défaut
                auditHelper.audit(exchange)
                    .desc("Retour au contexte normal")
                    .status("NORMAL")
                    .data("Data")
                    .send();
            });
        
        // ============================================================
        // EXEMPLE 5: Ajouter du tracking inline
        // ============================================================
        from("direct:example5-tracking")
            .routeId("example5-route")
            
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Validation avec tracking")
                    .status("VALIDATING")
                    .data("Data")
                    .tracking("step:validation")
                    .tracking("validator:SchemaValidator")
                    .tracking("schemaVersion:1.2")
                    .send();
            })
            
            .to("direct:process")
            
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                auditHelper.audit(exchange)
                    .desc("Traitement commande")
                    .status("PROCESSED")
                    .data(order)
                    .tracking("orderId:" + order.getId())
                    .tracking("amount:" + order.getAmount())
                    .tracking("step:completed")
                    .send();
            });
        
        // ============================================================
        // EXEMPLE 6: Pattern recommandé - Complet
        // ============================================================
        from("cxf:bean:soapEndpoint")
            .routeId("order-processing-complete")
            
            // === INITIALISATION ===
            .process(exchange -> {
                // Créer le ProcessContext avec vos valeurs métier
                ProcessContext ctx = new ProcessContext();
                ctx.setProcessId(System.currentTimeMillis());
                ctx.setProjectName("ECommerce-OrderManagement");
                ctx.setEngineName("order-processing-v3");
                ctx.setRestartedFromCheckpoint(false);
                
                // CustomId = votre format
                String orderId = exchange.getIn().getHeader("orderId", String.class);
                String executionId = "EXEC-" + orderId + "-" + System.currentTimeMillis();
                ctx.setCustomId(executionId);
                
                // Tracking info initial
                ctx.getTrackingInfo().add("userId:" + exchange.getIn().getHeader("userId"));
                ctx.getTrackingInfo().add("source:SOAP");
                ctx.getTrackingInfo().add("clientIp:" + exchange.getIn().getHeader("X-Forwarded-For"));
                ctx.getTrackingInfo().add("startTime:" + System.currentTimeMillis());
                
                // Sauvegarder dans l'exchange
                auditHelper.initProcessContext(exchange, ctx);
                exchange.setProperty("startTime", System.currentTimeMillis());
                exchange.setProperty("executionId", executionId);
            })
            
            // === AUDIT RÉCEPTION ===
            .process(exchange -> {
                String xmlInput = exchange.getIn().getBody(String.class);
                
                auditHelper.audit(exchange)
                    .desc("Réception message SOAP")
                    .status("RECEIVED")
                    .data(xmlInput)
                    .meta("messageSize", xmlInput.length())
                    .meta("contentType", "application/xml")
                    .tracking("step:reception")
                    .send();
            })
            
            .unmarshal().jaxb()
            
            // === VALIDATION ===
            .process(exchange -> {
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                ctx.getTrackingInfo().add("step:validation");
                
                auditHelper.audit(exchange)
                    .desc("Validation schema")
                    .status("VALIDATING")
                    .data("Validation XSD en cours")
                    .meta("schemaVersion", "1.2")
                    .send();
            })
            
            .to("direct:validation")
            
            // === TRAITEMENT MÉTIER ===
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // Enrichir le ProcessContext avec l'orderId
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                ctx.getTrackingInfo().add("orderId:" + order.getId());
                ctx.getTrackingInfo().add("customerId:" + order.getCustomerId());
                ctx.getTrackingInfo().add("step:business-processing");
                
                auditHelper.audit(exchange)
                    .desc("Traitement commande")
                    .status("PROCESSING")
                    .data(order)
                    .ref("orderId", order.getId())
                    .ref("customerId", order.getCustomerId())
                    .meta("orderType", order.getType())
                    .meta("amount", order.getAmount())
                    .send();
            })
            
            .to("bean:orderService?method=process")
            
            // === PERSISTANCE ===
            .process(exchange -> {
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                ctx.getTrackingInfo().add("step:persistence");
                
                auditHelper.audit(exchange)
                    .desc("Sauvegarde en base")
                    .status("PERSISTING")
                    .data("Insertion dans PostgreSQL")
                    .meta("database", "PostgreSQL")
                    .meta("table", "orders")
                    .send();
            })
            
            .to("jpa:Order")
            
            // === FIN ===
            .process(exchange -> {
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                ctx.getTrackingInfo().add("step:completed");
                ctx.getTrackingInfo().add("endTime:" + System.currentTimeMillis());
                
                auditHelper.audit(exchange)
                    .desc("Traitement terminé avec succès")
                    .status("SUCCESS")
                    .data("Commande traitée et confirmée")
                    .forceAudit(true)
                    .send();
            })
            
            .marshal().jaxb();
        
        // === GESTION D'ERREUR ===
        onException(Exception.class)
            .process(exchange -> {
                Exception error = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                
                // Enrichir le ProcessContext avec l'erreur
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                if (ctx != null) {
                    ctx.getTrackingInfo().add("error:" + error.getClass().getSimpleName());
                    ctx.getTrackingInfo().add("errorMessage:" + error.getMessage());
                }
                
                auditHelper.audit(exchange)
                    .desc("Erreur technique")
                    .status("ERROR")
                    .data(error.getMessage())
                    .meta("errorClass", error.getClass().getSimpleName())
                    .forceAudit(true)
                    .send();
            })
            .handled(true);
    }
}
