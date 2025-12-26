package fr.lbp.routes;

import fr.lbp.lib.audit.TibcoAuditHelper;
import fr.lbp.lib.error.TibcoErrorHelper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ErrorAuditStrategies extends RouteBuilder {
    
    @Autowired
    private TibcoAuditHelper auditHelper;
    
    @Autowired
    private TibcoErrorHelper errorHelper;
    
    @Override
    public void configure() {
        
        // ============================================================
        // STRATÉGIE 1: Audit automatique (recommandé pour la plupart des cas)
        // ============================================================
        from("direct:strategy1-auto-audit")
            .routeId("strategy1-auto-audit")
            
            .onException(Exception.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // ✅ L'erreur sera auditée automatiquement
                    errorHelper.error(exchange, ex)
                        .msgCode("ERR-001")
                        .meta("severity", "HIGH")
                        .send();
                    // Résultat: 1 erreur envoyée + 1 audit automatique créé
                })
                .handled(true)
            .end();
        
        // ============================================================
        // STRATÉGIE 2: Désactiver l'audit pour une erreur spécifique
        // ============================================================
        from("direct:strategy2-skip-audit")
            .routeId("strategy2-skip-audit")
            
            .onException(MinorException.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // ✅ Erreur technique enregistrée SANS audit
                    // (car c'est une erreur mineure qu'on ne veut pas auditer)
                    errorHelper.error(exchange, ex)
                        .msgCode("WARN-001")
                        .meta("severity", "LOW")
                        .skipAudit()  // ✅ Désactive l'audit automatique
                        .send();
                    // Résultat: 1 erreur envoyée, 0 audit
                })
                .handled(true)
            .end();
        
        // ============================================================
        // STRATÉGIE 3: Audit personnalisé + erreur
        // ============================================================
        from("direct:strategy3-custom-audit")
            .routeId("strategy3-custom-audit")
            
            .onException(BusinessException.class)
                .process(exchange -> {
                    BusinessException ex = (BusinessException) exchange.getProperty(
                        Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    Order order = exchange.getIn().getBody(Order.class);
                    
                    // ✅ Audit personnalisé AVANT l'erreur
                    auditHelper.audit(exchange)
                        .desc("Erreur métier: validation échouée")
                        .status("BUSINESS_ERROR")
                        .data(order)
                        .ref("orderId", order.getId())
                        .meta("validationRule", ex.getRuleName())
                        .meta("expectedValue", ex.getExpectedValue())
                        .meta("actualValue", ex.getActualValue())
                        .send();
                    
                    // ✅ Erreur technique SANS audit auto (pour éviter le doublon)
                    errorHelper.error(exchange, ex)
                        .msgCode("BUS-001")
                        .data(order)
                        .skipAudit()  // ✅ Évite le doublon d'audit
                        .send();
                    // Résultat: 1 audit personnalisé + 1 erreur
                })
                .handled(true)
            .end();
        
        // ============================================================
        // STRATÉGIE 4: Audit + Erreur avec données différentes
        // ============================================================
        from("direct:strategy4-different-data")
            .routeId("strategy4-different-data")
            
            .onException(Exception.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    Order order = exchange.getIn().getBody(Order.class);
                    
                    // ✅ Audit métier avec données métier
                    auditHelper.audit(exchange)
                        .desc("Erreur lors du traitement de la commande")
                        .status("ERROR")
                        .data(order)  // Données métier complètes
                        .ref("orderId", order.getId())
                        .ref("customerId", order.getCustomerId())
                        .meta("orderAmount", order.getAmount())
                        .send();
                    
                    // ✅ Erreur technique avec stack trace
                    errorHelper.error(exchange, ex)
                        .msgCode("TECH-500")
                        .skipAudit()  // Pas de doublon
                        .send();
                    // Résultat: 1 audit métier + 1 erreur technique
                })
                .handled(true)
            .end();
        
        // ============================================================
        // STRATÉGIE 5: Erreur critique = audit forcé même si désactivé
        // ============================================================
        from("direct:strategy5-force-audit")
            .routeId("strategy5-force-audit")
            
            .onException(CriticalException.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // ✅ Audit forcé pour erreur critique
                    auditHelper.audit(exchange)
                        .desc("ERREUR CRITIQUE - Système en échec")
                        .status("CRITICAL_ERROR")
                        .data(ex.getMessage())
                        .meta("severity", "CRITICAL")
                        .forceAudit(true)  // ✅ Force l'audit
                        .send();
                    
                    // ✅ Erreur avec audit auto désactivé (pour éviter doublon)
                    errorHelper.error(exchange, ex)
                        .msgCode("CRIT-999")
                        .meta("severity", "CRITICAL")
                        .meta("requiresImmediateAction", "true")
                        .skipAudit()
                        .send();
                })
                .handled(true)
                .setBody(constant("Critical error occurred"))
            .end();
        
        // ============================================================
        // STRATÉGIE 6: Pattern complet recommandé
        // ============================================================
        from("cxf:bean:soapEndpoint")
            .routeId("complete-error-audit-strategy")
            
            .process(exchange -> {
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
            .to("direct:validation")
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
            
            // === ERREURS MÉTIER ===
            // Auditées manuellement avec détails métier
            .onException(ValidationException.class)
                .process(exchange -> {
                    ValidationException ex = (ValidationException) exchange.getProperty(
                        Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // Audit métier détaillé
                    auditHelper.audit(exchange)
                        .desc("Validation métier échouée")
                        .status("VALIDATION_ERROR")
                        .data(exchange.getIn().getBody())
                        .meta("errorType", "BUSINESS")
                        .meta("validationRule", ex.getRuleName())
                        .send();
                    
                    // Erreur technique sans audit auto
                    errorHelper.error(exchange, ex)
                        .msgCode("VAL-001")
                        .skipAudit()
                        .send();
                })
                .handled(true)
            .end()
            
            // === ERREURS TECHNIQUES ===
            // Audit automatique activé (simple et efficace)
            .onException(Exception.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // ✅ Erreur avec audit automatique
                    // Pas besoin d'auditer manuellement
                    errorHelper.error(exchange, ex)
                        .msgCode("TECH-500")
                        .meta("severity", "HIGH")
                        .meta("errorType", "TECHNICAL")
                        // .withAudit() est implicite si enabled=true
                        .send();
                    // Résultat: 1 erreur + 1 audit automatique
                })
                .handled(true)
            .end();
        
        // ============================================================
        // STRATÉGIE 7: Configuration dynamique per-route
        // ============================================================
        from("direct:strategy7-dynamic-config")
            .routeId("strategy7-dynamic")
            
            .onException(Exception.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    String errorSeverity = determineErrorSeverity(ex);
                    
                    ErrorBuilder errorBuilder = errorHelper.error(exchange, ex)
                        .msgCode("DYN-001")
                        .meta("severity", errorSeverity);
                    
                    // ✅ Auditer seulement si severity >= HIGH
                    if ("HIGH".equals(errorSeverity) || "CRITICAL".equals(errorSeverity)) {
                        errorBuilder.withAudit();  // Force l'audit
                    } else {
                        errorBuilder.skipAudit();  // Skip l'audit
                    }
                    
                    errorBuilder.send();
                })
                .handled(true)
            .end();
    }
    
    private String determineErrorSeverity(Exception ex) {
        if (ex instanceof CriticalException) return "CRITICAL";
        if (ex instanceof HighPriorityException) return "HIGH";
        if (ex instanceof MediumPriorityException) return "MEDIUM";
        return "LOW";
    }
}
