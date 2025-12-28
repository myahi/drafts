package fr.lbp.routes;

import fr.lbp.lib.audit.TibcoAuditHelper;
import fr.lbp.lib.error.TibcoErrorHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * GUIDE DES MEILLEURES PRATIQUES DE LOGGING
 * 
 * Ce fichier démontre les bonnes pratiques de logging dans l'application.
 * Suivez ces patterns pour un logging efficace et maintenable.
 */
@Slf4j
@Component
public class LoggingBestPractices extends RouteBuilder {
    
    @Autowired
    private TibcoAuditHelper auditHelper;
    
    @Autowired
    private TibcoErrorHelper errorHelper;
    
    @Value("${logging.business-events.enabled:true}")
    private boolean businessLoggingEnabled;
    
    @Override
    public void configure() {
        
        // ============================================================
        // PATTERN PRINCIPAL: Route avec logging optimal
        // ============================================================
        from("cxf:bean:soapEndpoint")
            .routeId("order-processing-with-logging")
            
            // ========== INIT: Setup MDC ==========
            .process(exchange -> {
                String correlationId = UUID.randomUUID().toString();
                String userId = exchange.getIn().getHeader("userId", String.class);
                String executionId = "EXEC-" + System.currentTimeMillis();
                
                // ✅ TOUJOURS utiliser MDC pour la traçabilité
                MDC.put("correlationId", correlationId);
                MDC.put("userId", userId);
                MDC.put("executionId", executionId);
                
                exchange.setProperty("correlationId", correlationId);
                exchange.setProperty("executionId", executionId);
                exchange.setProperty("startTime", System.currentTimeMillis());
                
                // ✅ Log INFO pour le début du traitement (TOUJOURS)
                log.info("Processing started - userId={}, executionId={}", userId, executionId);
            })
            
            // ========== AUDIT: Envoyé vers EMS (PAS loggé sauf en DEV) ==========
            .process(exchange -> {
                auditHelper.audit(exchange)
                    .desc("Réception message SOAP")
                    .status("RECEIVED")
                    .data(exchange.getIn().getBody(String.class))
                    .send();
                // Le log est géré par TibcoAuditHelper selon audit.logging.enabled
            })
            
            .unmarshal().jaxb()
            
            // ========== LOG DEBUG: Détails techniques (visible en DEV) ==========
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // ✅ Log DEBUG pour les détails techniques
                log.debug("Order unmarshalled - orderId={}, itemsCount={}", 
                    order.getId(), order.getItems().size());
            })
            
            // ========== VALIDATION ==========
            .to("direct:validation")
            
            // ========== LOG MÉTIER: Événements business importants ==========
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // ✅ Log INFO pour les événements métier importants
                if (businessLoggingEnabled && order.getAmount() > 10000) {
                    log.info("High-value order detected - orderId={}, amount={}", 
                        order.getId(), order.getAmount());
                }
                
                // ✅ Log DEBUG pour tous les autres cas
                log.debug("Processing order - orderId={}, customerId={}", 
                    order.getId(), order.getCustomerId());
            })
            
            .to("bean:orderService?method=process")
            
            // ========== MÉTRIQUES DE PERFORMANCE ==========
            .process(exchange -> {
                Long startTime = exchange.getProperty("startTime", Long.class);
                Long duration = System.currentTimeMillis() - startTime;
                Order order = exchange.getIn().getBody(Order.class);
                
                // ✅ Log WARN si le traitement est lent
                if (duration > 5000) {
                    log.warn("Slow processing detected - orderId={}, duration={}ms, threshold=5000ms", 
                        order.getId(), duration);
                }
                
                // ✅ Log INFO pour le succès (TOUJOURS)
                log.info("Processing completed successfully - orderId={}, duration={}ms", 
                    order.getId(), duration);
                
                // Audit de fin
                auditHelper.audit(exchange)
                    .desc("Traitement terminé")
                    .status("SUCCESS")
                    .data(order)
                    .send();
            })
            
            // ========== CLEANUP: Toujours nettoyer le MDC ==========
            .process(exchange -> MDC.clear())
            
            .marshal().jaxb()
            
            // ============================================================
            // GESTION DES ERREURS MÉTIER
            // ============================================================
            .onException(ValidationException.class)
                .process(exchange -> {
                    ValidationException ex = (ValidationException) exchange.getProperty(
                        Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // ✅ Log WARN pour les erreurs métier (non techniques)
                    log.warn("Business validation failed - rule={}, orderId={}", 
                        ex.getRuleName(), ex.getOrderId());
                    
                    // Audit détaillé
                    auditHelper.audit(exchange)
                        .desc("Erreur de validation métier")
                        .status("VALIDATION_ERROR")
                        .data(ex.getMessage())
                        .meta("validationRule", ex.getRuleName())
                        .send();
                    
                    // Erreur (loggée par TibcoErrorHelper)
                    errorHelper.error(exchange, ex)
                        .msgCode("VAL-001")
                        .skipAudit()  // Déjà audité ci-dessus
                        .send();
                    
                    MDC.clear();
                })
                .handled(true)
            .end()
            
            // ============================================================
            // GESTION DES ERREURS TECHNIQUES
            // ============================================================
            .onException(Exception.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    // ✅ Log ERROR pour les erreurs techniques (TOUJOURS)
                    log.error("Technical error occurred - message={}, class={}", 
                        ex.getMessage(), ex.getClass().getSimpleName());
                    
                    // Erreur avec audit automatique
                    errorHelper.error(exchange, ex)
                        .msgCode("TECH-500")
                        .meta("severity", "HIGH")
                        .send();
                    // Le log et l'audit sont gérés par TibcoErrorHelper
                    
                    MDC.clear();
                })
                .handled(true)
            .end();
        
        // ============================================================
        // EXEMPLES DE LOGGING PAR CAS D'USAGE
        // ============================================================
        
        // ========== CAS 1: Données sensibles → JAMAIS logger ==========
        from("direct:payment-processing")
            .routeId("payment-with-sanitization")
            
            .process(exchange -> {
                Payment payment = exchange.getIn().getBody(Payment.class);
                
                // ❌ JAMAIS faire ça
                // log.info("Payment: {}", payment.toString());  // Pourrait logger le numéro de carte !
                
                // ✅ Logger seulement les infos non sensibles
                log.info("Payment processing - paymentId={}, amount={}, method={}, last4={}", 
                    payment.getId(),
                    payment.getAmount(),
                    payment.getMethod(),
                    maskCardNumber(payment.getCardNumber()));
            });
        
        // ========== CAS 2: Volume élevé → Log conditionnel ==========
        from("direct:high-volume-processing")
            .routeId("high-volume-with-sampling")
            
            .process(exchange -> {
                Message msg = exchange.getIn().getBody(Message.class);
                
                // ✅ Logger seulement 1 message sur 100 (sampling)
                if (System.currentTimeMillis() % 100 == 0) {
                    log.info("Sample message - type={}, size={}", 
                        msg.getType(), msg.getSize());
                }
                
                // ✅ Ou logger seulement les cas spéciaux
                if (msg.getPriority() > 5) {
                    log.info("High-priority message - id={}, priority={}", 
                        msg.getId(), msg.getPriority());
                }
            });
        
        // ========== CAS 3: Debug temporaire → Niveau DEBUG ==========
        from("direct:debugging-issues")
            .routeId("debug-investigation")
            
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // ✅ Log DEBUG pour investigation (désactivé en PROD)
                log.debug("DEBUG - Order full details: {}", order);
                log.debug("DEBUG - Headers: {}", exchange.getIn().getHeaders());
                log.debug("DEBUG - Properties: {}", exchange.getProperties());
                
                // Ces logs ne seront visibles qu'en DEV (logging.level.fr.lbp=DEBUG)
            });
        
        // ========== CAS 4: Métriques structurées → Format parsable ==========
        from("direct:metrics-logging")
            .routeId("structured-metrics")
            
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                Long startTime = exchange.getProperty("startTime", Long.class);
                Long duration = System.currentTimeMillis() - startTime;
                
                // ✅ Log structuré pour parsing automatique (ELK, Splunk)
                log.info("ORDER_PROCESSED orderId={} customerId={} amount={} currency={} items={} duration={}ms status=SUCCESS",
                    order.getId(),
                    order.getCustomerId(),
                    order.getAmount(),
                    order.getCurrency(),
                    order.getItems().size(),
                    duration);
                
                // Ce format peut être parsé par des regex pour monitoring
            });
        
        // ========== CAS 5: Logging selon l'environnement ==========
        from("direct:environment-specific-logging")
            .routeId("env-specific")
            
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // ✅ En DEV: Log verbeux
                if (log.isDebugEnabled()) {
                    log.debug("Full order details: order={}, headers={}", 
                        order, exchange.getIn().getHeaders());
                }
                
                // ✅ En PROD: Log minimal
                log.info("Order processed - orderId={}, status={}", 
                    order.getId(), "SUCCESS");
            });
    }
    
    // ============================================================
    // MÉTHODES UTILITAIRES
    // ============================================================
    
    /**
     * Masque un numéro de carte bancaire
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}

/**
 * ============================================================
 * RÉCAPITULATIF DES BONNES PRATIQUES
 * ============================================================
 * 
 * ✅ À FAIRE:
 * 
 * 1. TOUJOURS utiliser MDC pour correlationId, userId, executionId
 * 2. Log INFO pour: début/fin traitement, événements métier importants
 * 3. Log WARN pour: erreurs métier, performance dégradée
 * 4. Log ERROR pour: erreurs techniques, échecs critiques
 * 5. Log DEBUG pour: détails techniques, investigation
 * 6. Nettoyer le MDC (MDC.clear()) en fin de traitement
 * 7. Logger les métriques de performance
 * 8. Utiliser des logs structurés (key=value) pour parsing
 * 
 * ❌ À ÉVITER:
 * 
 * 1. JAMAIS logger de données sensibles (mots de passe, cartes, etc.)
 * 2. Ne pas logger les audits en PROD (déjà dans EMS)
 * 3. Ne pas logger chaque message en volume élevé (sampling)
 * 4. Ne pas dupliquer logs et audits
 * 5. Ne pas logger des stacktraces en INFO/WARN
 * 6. Ne pas oublier de nettoyer le MDC
 * 
 * ============================================================
 * CONFIGURATION RECOMMANDÉE PAR ENVIRONNEMENT
 * ============================================================
 * 
 * DEV:
 *   logging.level.fr.lbp: DEBUG
 *   audit.logging.enabled: true
 *   error.logging.include-stacktrace: true
 * 
 * PROD:
 *   logging.level.fr.lbp: INFO
 *   audit.logging.enabled: false  (déjà dans EMS)
 *   error.logging.include-stacktrace: false
 * 
 * ============================================================
 */
