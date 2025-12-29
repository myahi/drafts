package fr.lbp.routes;

import fr.lbp.lib.audit.TibcoAuditHelper;
import fr.lbp.lib.error.TibcoErrorHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * EXEMPLES D'UTILISATION AVEC GESTION AUTOMATIQUE DU CONTEXTE
 * 
 * Le contexte (ProcessContext, MDC, startTime) est géré automatiquement
 * par TibcoAuditHelper. Vos routes sont beaucoup plus simples !
 */
@Slf4j
@Component
public class SimplifiedRoutesWithAutoContext extends RouteBuilder {
    
    @Autowired
    private TibcoAuditHelper auditHelper;
    
    @Autowired
    private TibcoErrorHelper errorHelper;
    
    @Override
    public void configure() {
        
        // ============================================================
        // EXEMPLE 1: Route ultra-simple (contexte automatique)
        // ============================================================
        from("cxf:bean:soapEndpoint")
            .routeId("order-processing-simple")
            
            // ✅ Contexte initialisé automatiquement au premier audit
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Réception message SOAP")
                    .status("RECEIVED")
                    .data(exchange.getIn().getBody(String.class))
                    .send();
                // Le ProcessContext, MDC, et startTime sont créés automatiquement !
            })
            
            .unmarshal().jaxb()
            .to("direct:validation")
            
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // ✅ Pas besoin de gérer le contexte
                auditHelper.audit(exchange)
                    .desc("Validation réussie")
                    .status("VALIDATED")
                    .data(order)
                    .send();
                // La durée est calculée automatiquement !
            })
            
            .to("bean:orderService?method=process")
            
            .process(exchange -> {
                // ✅ Toujours pas besoin de gérer le contexte
                auditHelper.audit(exchange)
                    .desc("Traitement terminé")
                    .status("SUCCESS")
                    .data(exchange.getIn().getBody())
                    .send();
            })
            
            // ✅ Cleanup automatique en cas d'erreur (via onException)
            .marshal().jaxb();
        
        // ============================================================
        // EXEMPLE 2: Route avec initialisation explicite
        // (si vous voulez contrôler le nom du projet)
        // ============================================================
        from("direct:order-with-custom-project")
            .routeId("order-custom-project")
            
            // ✅ Initialiser explicitement avec un nom de projet personnalisé
            .process(exchange -> {
                auditHelper.initializeContext(exchange, "OrderManagement-V2", "order-processor");
            })
            
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Début traitement")
                    .status("STARTED")
                    .data("Processing order")
                    .send();
                // Utilisera le projet "OrderManagement-V2" défini ci-dessus
            })
            
            .to("bean:orderService");
        
        // ============================================================
        // EXEMPLE 3: Ajout de tracking info inline
        // ============================================================
        from("direct:order-with-tracking")
            .routeId("order-with-tracking")
            
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // ✅ Ajouter du tracking directement dans l'audit
                auditHelper.audit(exchange)
                    .desc("Traitement commande")
                    .status("PROCESSING")
                    .data(order)
                    .tracking("orderId:" + order.getId())
                    .tracking("amount:" + order.getAmount())
                    .tracking("priority:HIGH")
                    .send();
            });
        
        // ============================================================
        // EXEMPLE 4: Enrichissement du contexte en cours de route
        // ============================================================
        from("direct:order-with-enrichment")
            .routeId("order-with-enrichment")
            
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Réception")
                    .status("RECEIVED")
                    .data("Message reçu")
                    .send();
            })
            
            .unmarshal().jaxb()
            
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // ✅ Enrichir le contexte avec des infos métier
                auditHelper.addTracking(exchange, "orderId:" + order.getId());
                auditHelper.addTracking(exchange, "customerId:" + order.getCustomerId());
                
                auditHelper.audit(exchange)
                    .desc("Validation commande")
                    .status("VALIDATING")
                    .data(order)
                    .send();
                // Le ProcessContext contient maintenant orderId et customerId
            });
        
        // ============================================================
        // EXEMPLE 5: Consultation du contexte
        // ============================================================
        from("direct:order-check-context")
            .routeId("order-check-context")
            
            .process(exchange -> {
                // ✅ Vérifier si le contexte est initialisé
                if (!auditHelper.isContextInitialized(exchange)) {
                    log.warn("Context not initialized, initializing now");
                    auditHelper.initializeContext(exchange);
                }
                
                // ✅ Récupérer la durée
                Long duration = auditHelper.getDuration(exchange);
                log.info("Processing time so far: {}ms", duration);
                
                // ✅ Récupérer le ProcessContext
                ProcessContext ctx = auditHelper.getProcessContext(exchange);
                log.info("ExecutionId: {}", ctx.getCustomId());
            });
        
        // ============================================================
        // EXEMPLE 6: Gestion des erreurs avec cleanup
        // ============================================================
        from("direct:order-with-error-handling")
            .routeId("order-error-handling")
            
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Début traitement")
                    .status("STARTED")
                    .data("Start")
                    .send();
            })
            
            .to("direct:risky-processing")
            
            .onException(Exception.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // ✅ Audit de l'erreur (contexte toujours disponible)
                    auditHelper.audit(exchange)
                        .desc("Erreur lors du traitement")
                        .status("ERROR")
                        .data(ex.getMessage())
                        .send();
                    
                    errorHelper.error(exchange, ex)
                        .msgCode("ERR-001")
                        .send();
                    
                    // ✅ Cleanup automatique du contexte
                    auditHelper.cleanupContext(exchange);
                })
                .handled(true)
            .end();
        
        // ============================================================
        // EXEMPLE 7: Route complète - Pattern recommandé
        // ============================================================
        from("cxf:bean:soapEndpoint")
            .routeId("complete-order-processing")
            
            // === DÉBUT ===
            .process(exchange -> {
                // ✅ Init explicite si besoin de personnaliser
                // Sinon, l'init auto se fera au premier audit
                auditHelper.initializeContext(exchange, "OrderManagement");
                
                auditHelper.audit(exchange)
                    .desc("Réception message SOAP")
                    .status("RECEIVED")
                    .data(exchange.getIn().getBody(String.class))
                    .send();
            })
            
            // === UNMARSHALLING ===
            .unmarshal().jaxb()
            
            // === VALIDATION ===
            .to("direct:validation")
            
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Validation terminée")
                    .status("VALIDATED")
                    .data(exchange.getIn().getBody())
                    .send();
            })
            
            // === ENRICHISSEMENT ===
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // Enrichir le contexte
                auditHelper.addTracking(exchange, "orderId:" + order.getId());
                auditHelper.addTracking(exchange, "step:enrichment");
                
                auditHelper.audit(exchange)
                    .desc("Enrichissement données client")
                    .status("ENRICHING")
                    .data(order)
                    .tracking("source:CRM")
                    .send();
            })
            
            .to("direct:enrichCustomerData")
            
            // === TRAITEMENT MÉTIER ===
            .to("bean:orderService?method=process")
            
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                auditHelper.audit(exchange)
                    .desc("Traitement terminé")
                    .status("SUCCESS")
                    .data(order)
                    .meta("orderId", order.getId())
                    .meta("amount", order.getAmount())
                    .tracking("step:completed")
                    .send();
            })
            
            // === CLEANUP ===
            .process(exchange -> {
                // ✅ Cleanup du contexte en fin de traitement
                auditHelper.cleanupContext(exchange);
            })
            
            .marshal().jaxb()
            
            // === GESTION ERREURS ===
            .onException(ValidationException.class)
                .process(exchange -> {
                    ValidationException ex = (ValidationException) exchange.getProperty(
                        Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    auditHelper.audit(exchange)
                        .desc("Erreur de validation")
                        .status("VALIDATION_ERROR")
                        .data(ex.getMessage())
                        .meta("validationRule", ex.getRuleName())
                        .send();
                    
                    errorHelper.error(exchange, ex)
                        .msgCode("VAL-001")
                        .skipAudit()
                        .send();
                    
                    auditHelper.cleanupContext(exchange);
                })
                .handled(true)
            .end()
            
            .onException(Exception.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    auditHelper.audit(exchange)
                        .desc("Erreur technique")
                        .status("ERROR")
                        .data(ex.getMessage())
                        .send();
                    
                    errorHelper.error(exchange, ex)
                        .msgCode("TECH-500")
                        .send();
                    
                    auditHelper.cleanupContext(exchange);
                })
                .handled(true)
            .end();
    }
}
