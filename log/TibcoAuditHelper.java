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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TibcoAuditHelper {
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    
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
    
    /**
     * Point d'entrée principal : crée un builder pré-configuré
     */
    public AuditBuilder audit(Exchange exchange) {
        return new AuditBuilder(exchange, this);
    }
    
    /**
     * Envoie l'audit vers la destination configurée (SEDA ou EMS)
     */
    public void sendAudit(Audit audit) {
        try {
            // Choisir la destination selon la config
            if ("ems".equalsIgnoreCase(auditDestination) && emsSender != null) {
                // ✅ Envoyer vers EMS
                emsSender.sendAudit(audit);
                log.debug("Audit sent to EMS: description={}, status={}", 
                    audit.getAuditInfo().getDescription(), 
                    audit.getAuditInfo().getStatut());
            } else {
                // ✅ Envoyer vers SEDA (par défaut)
                producerTemplate.asyncSendBody("seda:audit", audit);
                log.debug("Audit sent to SEDA: description={}, status={}", 
                    audit.getAuditInfo().getDescription(), 
                    audit.getAuditInfo().getStatut());
            }
            
            // Logger conditionnellement selon la config
            if (auditLoggingEnabled) {
                log.info("Audit: desc={}, status={}, correlationId={}", 
                    audit.getAuditInfo().getDescription(),
                    audit.getAuditInfo().getStatut(),
                    audit.getProcessContext().getCustomId());
            }
            
        } catch (Exception e) {
            // ✅ TOUJOURS logger les échecs d'envoi
            log.error("CRITICAL: Failed to send audit event - description={}, status={}", 
                audit.getAuditInfo().getDescription(), 
                audit.getAuditInfo().getStatut(), e);
        }
    }
    
    /**
     * Extrait ou crée le ProcessContext
     */
    private ProcessContext extractProcessContext(Exchange exchange) {
        ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
        
        if (ctx == null) {
            ctx = createDefaultProcessContext(exchange);
            exchange.setProperty("processContext", ctx);
        }
        
        return ctx;
    }
    
    /**
     * Crée un ProcessContext par défaut
     */
    private ProcessContext createDefaultProcessContext(Exchange exchange) {
        ProcessContext ctx = new ProcessContext();
        ctx.setProcessId(System.currentTimeMillis());
        ctx.setProjectName(exchange.getContext().getName());
        ctx.setEngineName(exchange.getFromRouteId());
        ctx.setRestartedFromCheckpoint(false);
        
        String executionId = exchange.getProperty("executionId", String.class);
        if (executionId == null) {
            executionId = "EXEC-" + System.currentTimeMillis();
            exchange.setProperty("executionId", executionId);
        }
        ctx.setCustomId(executionId);
        
        ctx.getTrackingInfo().add("camelContext:" + exchange.getContext().getName());
        ctx.getTrackingInfo().add("routeId:" + exchange.getFromRouteId());
        ctx.getTrackingInfo().add("exchangeId:" + exchange.getExchangeId());
        
        return ctx;
    }
    
    /**
     * Permet d'initialiser un ProcessContext personnalisé
     */
    public void initProcessContext(Exchange exchange, ProcessContext customContext) {
        exchange.setProperty("processContext", customContext);
    }
    
    /**
     * Helper pour créer un ProcessContext avec valeurs de base
     */
    public ProcessContext createProcessContext(String projectName, String engineName) {
        ProcessContext ctx = new ProcessContext();
        ctx.setProcessId(System.currentTimeMillis());
        ctx.setProjectName(projectName);
        ctx.setEngineName(engineName);
        ctx.setRestartedFromCheckpoint(false);
        ctx.setCustomId("EXEC-" + System.currentTimeMillis());
        return ctx;
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
        private ProcessContext customProcessContext;
        
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
        
        public AuditBuilder processContext(ProcessContext ctx) {
            this.customProcessContext = ctx;
            return this;
        }
        
        public AuditBuilder tracking(String trackingInfo) {
            ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
            if (ctx == null) {
                ctx = helper.createDefaultProcessContext(exchange);
                exchange.setProperty("processContext", ctx);
            }
            ctx.getTrackingInfo().add(trackingInfo);
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
            Long startTime = exchange.getProperty("startTime", Long.class);
            if (startTime != null) {
                Long duration = System.currentTimeMillis() - startTime;
                metadatas.put("duration", duration.toString());
            }
            
            metadatas.put("exchangeId", exchange.getExchangeId());
            metadatas.put("routeId", exchange.getFromRouteId());
            
            String executionId = exchange.getProperty("executionId", String.class);
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
            
            ProcessContext processContext = customProcessContext != null ?
                customProcessContext :
                helper.extractProcessContext(exchange);
            
            Audit audit = new Audit();
            audit.setAuditInfo(auditInfo);
            audit.setProcessContext(processContext);
            audit.setForceAudit(forceAudit);
            
            helper.sendAudit(audit);
        }
    }
}
