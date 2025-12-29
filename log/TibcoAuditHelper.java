package fr.lbp.lib.audit;

import fr.lbp.lib.ems.EMSSenderHelper;
import fr.lbp.lib.model.audit.Audit;
import fr.lbp.lib.model.auditinfo.AuditInfo;
import fr.lbp.lib.model.auditinfo.Metadata;
import fr.lbp.lib.model.auditinfo.Metadatas;
import fr.lbp.lib.model.auditinfo.Reference;
import fr.lbp.lib.model.enginetypes.ProcessContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TibcoAuditHelper avec gestion automatique du contexte
 * 
 * Ce helper gère automatiquement:
 * - L'initialisation du ProcessContext
 * - La gestion du MDC (correlationId, userId, executionId)
 * - Le calcul de la durée
 * - Le nettoyage du contexte
 */
@Slf4j
@Component
public class TibcoAuditHelper {
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    
    private static final String CONTEXT_INITIALIZED_KEY = "auditContextInitialized";
    private static final String START_TIME_KEY = "startTime";
    private static final String PROCESS_CONTEXT_KEY = "processContext";
    private static final String EXECUTION_ID_KEY = "executionId";
    private static final String CORRELATION_ID_KEY = "correlationId";
    
    @Autowired
    private ProducerTemplate producerTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private EMSSenderHelper emsSender;
    
    @Value("${audit.logging.enabled:false}")
    private boolean auditLoggingEnabled;
    
    @Value("${audit.destination:seda}")
    private String auditDestination;
    
    @Value("${audit.context.auto-init:true}")
    private boolean autoInitContext;
    
    @Value("${audit.context.project-name:}")
    private String defaultProjectName;
    
    /**
     * Point d'entrée principal : crée un builder pré-configuré
     * Initialise automatiquement le contexte si nécessaire
     */
    public AuditBuilder audit(Exchange exchange) {
        // Initialiser automatiquement le contexte si pas encore fait
        if (autoInitContext && !isContextInitialized(exchange)) {
            initializeContext(exchange);
        }
        
        return new AuditBuilder(exchange, this);
    }
    
    /**
     * Initialise le contexte d'audit pour un exchange
     * À appeler au début d'une route ou fait automatiquement
     */
    public void initializeContext(Exchange exchange) {
        initializeContext(exchange, null, null);
    }
    
    /**
     * Initialise le contexte avec un nom de projet personnalisé
     */
    public void initializeContext(Exchange exchange, String projectName) {
        initializeContext(exchange, projectName, null);
    }
    
    /**
     * Initialise le contexte avec projet et engine personnalisés
     */
    public void initializeContext(Exchange exchange, String projectName, String engineName) {
        if (isContextInitialized(exchange)) {
            log.debug("Context already initialized for exchange {}", exchange.getExchangeId());
            return;
        }
        
        // Générer les IDs
        String correlationId = UUID.randomUUID().toString();
        String executionId = "EXEC-" + System.currentTimeMillis() + "-" + 
            UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        String userId = exchange.getIn().getHeader("userId", String.class);
        if (userId == null) {
            userId = exchange.getIn().getHeader("user-id", String.class);
        }
        if (userId == null) {
            userId = "UNKNOWN";
        }
        
        // Stocker dans l'Exchange
        exchange.setProperty(CORRELATION_ID_KEY, correlationId);
        exchange.setProperty(EXECUTION_ID_KEY, executionId);
        exchange.setProperty(START_TIME_KEY, System.currentTimeMillis());
        exchange.setProperty(CONTEXT_INITIALIZED_KEY, true);
        
        // Configurer le MDC
        MDC.put(CORRELATION_ID_KEY, correlationId);
        MDC.put("userId", userId);
        MDC.put(EXECUTION_ID_KEY, executionId);
        
        // Créer le ProcessContext
        ProcessContext ctx = new ProcessContext();
        ctx.setProcessId(System.currentTimeMillis());
        
        // Utiliser le projet fourni ou le défaut ou le contexte Camel
        if (projectName != null && !projectName.isEmpty()) {
            ctx.setProjectName(projectName);
        } else if (defaultProjectName != null && !defaultProjectName.isEmpty()) {
            ctx.setProjectName(defaultProjectName);
        } else {
            ctx.setProjectName(exchange.getContext().getName());
        }
        
        // Utiliser l'engine fourni ou la route courante
        if (engineName != null && !engineName.isEmpty()) {
            ctx.setEngineName(engineName);
        } else {
            ctx.setEngineName(exchange.getFromRouteId());
        }
        
        ctx.setRestartedFromCheckpoint(false);
        ctx.setCustomId(executionId);
        
        // Tracking info initial
        ctx.getTrackingInfo().add("camelContext:" + exchange.getContext().getName());
        ctx.getTrackingInfo().add("routeId:" + exchange.getFromRouteId());
        ctx.getTrackingInfo().add("exchangeId:" + exchange.getExchangeId());
        ctx.getTrackingInfo().add("userId:" + userId);
        ctx.getTrackingInfo().add("initializedAt:" + ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
        
        exchange.setProperty(PROCESS_CONTEXT_KEY, ctx);
        
        log.info("Audit context initialized - executionId={}, userId={}, routeId={}", 
            executionId, userId, exchange.getFromRouteId());
    }
    
    /**
     * Nettoie le contexte d'audit
     * À appeler à la fin d'une route ou fait automatiquement en cas d'erreur
     */
    public void cleanupContext(Exchange exchange) {
        MDC.clear();
        exchange.removeProperty(CONTEXT_INITIALIZED_KEY);
        log.debug("Audit context cleaned up for exchange {}", exchange.getExchangeId());
    }
    
    /**
     * Vérifie si le contexte est initialisé
     */
    public boolean isContextInitialized(Exchange exchange) {
        return exchange.getProperty(CONTEXT_INITIALIZED_KEY, Boolean.class) != null;
    }
    
    /**
     * Récupère le ProcessContext de l'exchange
     */
    public ProcessContext getProcessContext(Exchange exchange) {
        return exchange.getProperty(PROCESS_CONTEXT_KEY, ProcessContext.class);
    }
    
    /**
     * Enrichit le ProcessContext avec des tracking info
     */
    public void addTracking(Exchange exchange, String trackingInfo) {
        ProcessContext ctx = getProcessContext(exchange);
        if (ctx != null) {
            ctx.getTrackingInfo().add(trackingInfo);
            log.debug("Added tracking info: {}", trackingInfo);
        }
    }
    
    /**
     * Calcule la durée depuis le début du traitement
     */
    public Long getDuration(Exchange exchange) {
        Long startTime = exchange.getProperty(START_TIME_KEY, Long.class);
        return startTime != null ? System.currentTimeMillis() - startTime : null;
    }
    
    /**
     * Envoie l'audit vers la destination configurée
     */
    public void sendAudit(Audit audit) {
        try {
            if ("ems".equalsIgnoreCase(auditDestination) && emsSender != null) {
                emsSender.sendAudit(audit);
                log.debug("Audit sent to EMS: description={}, status={}", 
                    audit.getAuditInfo().getDescription(), 
                    audit.getAuditInfo().getStatut());
            } else {
                producerTemplate.asyncSendBody("seda:audit", audit);
                log.debug("Audit sent to SEDA: description={}, status={}", 
                    audit.getAuditInfo().getDescription(), 
                    audit.getAuditInfo().getStatut());
            }
            
            if (auditLoggingEnabled) {
                log.info("Audit: desc={}, status={}, correlationId={}", 
                    audit.getAuditInfo().getDescription(),
                    audit.getAuditInfo().getStatut(),
                    audit.getProcessContext().getCustomId());
            }
            
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send audit event - description={}, status={}", 
                audit.getAuditInfo().getDescription(), 
                audit.getAuditInfo().getStatut(), e);
        }
    }
    
    /**
     * Builder fluent pour créer des audits TIBCO
     */
    public static class AuditBuilder {
        private final Exchange exchange;
        private final TibcoAuditHelper helper;
        
        private String description;
        private String data;
        private String statut;
        private final List<Reference> references = new ArrayList<>();
        private final Map<String, String> metadatas = new HashMap<>();
        private Boolean forceAudit;
        private final List<String> trackingInfoToAdd = new ArrayList<>();
        
        AuditBuilder(Exchange exchange, TibcoAuditHelper helper) {
            this.exchange = exchange;
            this.helper = helper;
        }
        
        public AuditBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public AuditBuilder desc(String description) {
            return description(description);
        }
        
        public AuditBuilder status(String statut) {
            this.statut = statut;
            return this;
        }
        
        public AuditBuilder data(Object data) {
            if (data instanceof String) {
                this.data = (String) data;
            } else {
                try {
                    this.data = helper.objectMapper.writeValueAsString(data);
                } catch (Exception e) {
                    log.error("Failed to serialize data", e);
                    this.data = data.toString();
                }
            }
            return this;
        }
        
        public AuditBuilder reference(String name, String value) {
            Reference ref = new Reference();
            ref.setName(name);
            ref.setValue(value);
            this.references.add(ref);
            return this;
        }
        
        public AuditBuilder ref(String name, String value) {
            return reference(name, value);
        }
        
        public AuditBuilder metadata(String key, String value) {
            this.metadatas.put(key, value);
            return this;
        }
        
        public AuditBuilder meta(String key, String value) {
            return metadata(key, value);
        }
        
        public AuditBuilder meta(String key, Object value) {
            this.metadatas.put(key, value != null ? value.toString() : null);
            return this;
        }
        
        public AuditBuilder metas(Map<String, Object> additionalMetadata) {
            if (additionalMetadata != null) {
                additionalMetadata.forEach((k, v) -> 
                    this.metadatas.put(k, v != null ? v.toString() : null));
            }
            return this;
        }
        
        public AuditBuilder forceAudit(boolean force) {
            this.forceAudit = force;
            return this;
        }
        
        /**
         * Ajoute une info de tracking qui sera ajoutée au ProcessContext
         */
        public AuditBuilder tracking(String trackingInfo) {
            this.trackingInfoToAdd.add(trackingInfo);
            return this;
        }
        
        public void send() {
            if (description == null || data == null) {
                throw new IllegalStateException(
                    "description and data are required. Use .description() and .data()");
            }
            
            // Créer AuditInfo
            AuditInfo auditInfo = new AuditInfo();
            auditInfo.setTimestamp(ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
            auditInfo.setDescription(description);
            auditInfo.setData(data);
            auditInfo.setStatut(statut);
            
            if (!references.isEmpty()) {
                auditInfo.setReference(references);
            }
            
            // Métadonnées automatiques
            Long duration = helper.getDuration(exchange);
            if (duration != null) {
                metadatas.put("duration", duration.toString());
            }
            
            metadatas.put("exchangeId", exchange.getExchangeId());
            metadatas.put("routeId", exchange.getFromRouteId());
            
            String executionId = exchange.getProperty(EXECUTION_ID_KEY, String.class);
            if (executionId != null) {
                metadatas.put("executionId", executionId);
            }
            
            if (!metadatas.isEmpty()) {
                Metadatas metadatasObj = new Metadatas();
                List<Metadata> metadataList = new ArrayList<>();
                metadatas.forEach((k, v) -> metadataList.add(new Metadata(k, v)));
                metadatasObj.setMetadata(metadataList);
                auditInfo.setMetadatas(metadatasObj);
            }
            
            // Récupérer le ProcessContext
            ProcessContext processContext = helper.getProcessContext(exchange);
            if (processContext == null) {
                log.warn("ProcessContext not found, creating default one");
                helper.initializeContext(exchange);
                processContext = helper.getProcessContext(exchange);
            }
            
            // Ajouter les tracking info demandés
            for (String tracking : trackingInfoToAdd) {
                processContext.getTrackingInfo().add(tracking);
            }
            
            // Créer l'audit
            Audit audit = new Audit();
            audit.setAuditInfo(auditInfo);
            audit.setProcessContext(processContext);
            audit.setForceAudit(forceAudit);
            
            helper.sendAudit(audit);
        }
    }
}
