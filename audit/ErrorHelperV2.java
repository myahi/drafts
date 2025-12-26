package fr.lbp.lib.error;

import com.mycompany.model.*;
import fr.lbp.lib.audit.TibcoAuditHelper;
import fr.lbp.lib.model.auditinfo.Metadata;
import fr.lbp.lib.model.auditinfo.Metadatas;
import fr.lbp.lib.model.enginetypes.ProcessContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ErrorHelperV2 {
    
    @Autowired
    private ProducerTemplate producerTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private TibcoAuditHelper auditHelper;
    
    @Value("${error.auto-audit.enabled:true}")
    private boolean autoAuditEnabled;
    
    /**
     * Point d'entrée principal : crée un builder pré-configuré
     */
    public ErrorBuilder error(Exchange exchange) {
        return new ErrorBuilder(exchange, this);
    }
    
    /**
     * Crée un ErrorBuilder à partir d'une exception
     */
    public ErrorBuilder error(Exchange exchange, Exception exception) {
        return new ErrorBuilder(exchange, this).fromException(exception);
    }
    
    /**
     * Crée un ErrorBuilder à partir de l'exception caught dans l'exchange
     */
    public ErrorBuilder errorFromExchange(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        if (exception == null) {
            exception = exchange.getException();
        }
        return error(exchange, exception);
    }
    
    /**
     * Envoie l'erreur vers la queue
     * Audite automatiquement si activé
     */
    public void sendError(Error error) {
        try {
            producerTemplate.asyncSendBody("seda:error", error);
            log.debug("Error sent: msg={}, class={}", 
                error.getErrorReport().getMsg(), 
                error.getErrorReport().getClazz());
        } catch (Exception e) {
            log.error("Failed to send error", e);
        }
    }
    
    /**
     * Audite l'erreur si l'audit automatique est activé
     */
    private void auditErrorIfEnabled(Exchange exchange, Error error, boolean skipAudit) {
        if (skipAudit || !autoAuditEnabled || auditHelper == null) {
            return;
        }
        
        try {
            ErrorReport errorReport = error.getErrorReport();
            
            auditHelper.audit(exchange)
                .desc("Erreur: " + errorReport.getMsg())
                .status("ERROR")
                .data(errorReport.getStackTrace())
                .meta("errorClass", errorReport.getClazz())
                .meta("errorFullClass", errorReport.getFullClass())
                .meta("errorMsgCode", errorReport.getMsgCode())
                .meta("processStack", errorReport.getProcessStack())
                .send();
            
            log.debug("Error automatically audited");
        } catch (Exception e) {
            log.error("Failed to auto-audit error", e);
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
     * Extrait la stacktrace d'une exception
     */
    private String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Builder fluent pour créer des erreurs TIBCO
     */
    public static class ErrorBuilder {
        private final Exchange exchange;
        private final TibcoErrorHelper helper;
        
        private String stackTrace;
        private String msg;
        private String fullClass;
        private String clazz;
        private String processStack;
        private String msgCode;
        private Object errorData;
        private Object mainData;
        private final List<Reference> references = new ArrayList<>();
        private final Map<String, String> metadatas = new HashMap<>();
        private ProcessContext customProcessContext;
        private boolean skipAudit = false;  // ✅ Nouveau flag
        
        ErrorBuilder(Exchange exchange, TibcoErrorHelper helper) {
            this.exchange = exchange;
            this.helper = helper;
        }
        
        /**
         * Initialise l'ErrorBuilder à partir d'une exception
         */
        public ErrorBuilder fromException(Exception exception) {
            this.stackTrace = helper.getStackTraceAsString(exception);
            this.msg = exception.getMessage() != null ? exception.getMessage() : "No message";
            this.fullClass = exception.getClass().getName();
            this.clazz = exception.getClass().getSimpleName();
            
            // Process stack depuis la route Camel
            this.processStack = exchange.getFromRouteId() + " -> " + 
                (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown");
            
            // Ajouter la cause si elle existe
            if (exception.getCause() != null) {
                this.metadatas.put("causeClass", exception.getCause().getClass().getName());
                this.metadatas.put("causeMessage", exception.getCause().getMessage());
            }
            
            return this;
        }
        
        /**
         * Définit le message d'erreur
         */
        public ErrorBuilder message(String msg) {
            this.msg = msg;
            return this;
        }
        
        /**
         * Alias pour message
         */
        public ErrorBuilder msg(String msg) {
            return message(msg);
        }
        
        /**
         * Définit la stackTrace
         */
        public ErrorBuilder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }
        
        /**
         * Définit la classe complète
         */
        public ErrorBuilder fullClass(String fullClass) {
            this.fullClass = fullClass;
            return this;
        }
        
        /**
         * Définit le nom de classe court
         */
        public ErrorBuilder clazz(String clazz) {
            this.clazz = clazz;
            return this;
        }
        
        /**
         * Définit le processStack
         */
        public ErrorBuilder processStack(String processStack) {
            this.processStack = processStack;
            return this;
        }
        
        /**
         * Définit le code de message
         */
        public ErrorBuilder msgCode(String msgCode) {
            this.msgCode = msgCode;
            return this;
        }
        
        /**
         * Définit les données dans ErrorReport
         */
        public ErrorBuilder errorData(Object data) {
            this.errorData = data;
            return this;
        }
        
        /**
         * Définit les données principales de l'erreur
         */
        public ErrorBuilder data(Object data) {
            this.mainData = data;
            return this;
        }
        
        /**
         * Ajoute une référence
         */
        public ErrorBuilder reference(String code, String codifier) {
            Reference ref = new Reference(code, codifier);
            this.references.add(ref);
            return this;
        }
        
        /**
         * Alias pour reference
         */
        public ErrorBuilder ref(String code, String codifier) {
            return reference(code, codifier);
        }
        
        /**
         * Ajoute une métadonnée
         */
        public ErrorBuilder metadata(String key, String value) {
            this.metadatas.put(key, value);
            return this;
        }
        
        /**
         * Alias pour metadata
         */
        public ErrorBuilder meta(String key, String value) {
            return metadata(key, value);
        }
        
        /**
         * Ajoute une métadonnée avec conversion automatique en String
         */
        public ErrorBuilder meta(String key, Object value) {
            this.metadatas.put(key, value != null ? value.toString() : null);
            return this;
        }
        
        /**
         * Ajoute plusieurs métadonnées
         */
        public ErrorBuilder metas(Map<String, Object> additionalMetadata) {
            if (additionalMetadata != null) {
                additionalMetadata.forEach((k, v) -> 
                    this.metadatas.put(k, v != null ? v.toString() : null));
            }
            return this;
        }
        
        /**
         * Permet de spécifier un ProcessContext personnalisé
         */
        public ErrorBuilder processContext(ProcessContext ctx) {
            this.customProcessContext = ctx;
            return this;
        }
        
        /**
         * Ajoute une info de tracking au ProcessContext
         */
        public ErrorBuilder tracking(String trackingInfo) {
            ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
            if (ctx == null) {
                ctx = helper.createDefaultProcessContext(exchange);
                exchange.setProperty("processContext", ctx);
            }
            ctx.getTrackingInfo().add(trackingInfo);
            return this;
        }
        
        /**
         * Désactive l'audit automatique pour cette erreur
         */
        public ErrorBuilder skipAudit() {
            this.skipAudit = true;
            return this;
        }
        
        /**
         * Active l'audit automatique pour cette erreur (si désactivé globalement)
         */
        public ErrorBuilder withAudit() {
            this.skipAudit = false;
            return this;
        }
        
        /**
         * Construit et envoie l'erreur
         */
        public void send() {
            // Valeurs par défaut si pas définies
            if (msg == null) {
                msg = "Unknown error";
            }
            if (stackTrace == null) {
                stackTrace = "No stack trace available";
            }
            if (fullClass == null) {
                fullClass = "unknown.UnknownException";
            }
            if (clazz == null) {
                clazz = "UnknownException";
            }
            if (processStack == null) {
                processStack = exchange.getFromRouteId();
            }
            
            // Créer ErrorReport
            ErrorReport errorReport = new ErrorReport();
            errorReport.setStackTrace(stackTrace);
            errorReport.setMsg(msg);
            errorReport.setFullClass(fullClass);
            errorReport.setClazz(clazz);
            errorReport.setProcessStack(processStack);
            errorReport.setMsgCode(msgCode);
            
            // Ajouter data dans ErrorReport si défini
            if (errorData != null) {
                AnyData anyData = new AnyData(errorData);
                errorReport.setData(anyData);
            }
            
            // Ajouter les métadonnées automatiques
            metadatas.put("exchangeId", exchange.getExchangeId());
            metadatas.put("routeId", exchange.getFromRouteId());
            
            String executionId = exchange.getProperty("executionId", String.class);
            if (executionId != null) {
                metadatas.put("executionId", executionId);
            }
            
            Long startTime = exchange.getProperty("startTime", Long.class);
            if (startTime != null) {
                Long duration = System.currentTimeMillis() - startTime;
                metadatas.put("duration", duration.toString());
            }
            
            // Convertir metadatas
            Metadatas metadatasObj = null;
            if (!metadatas.isEmpty()) {
                metadatasObj = new Metadatas();
                List<Metadata> metadataList = new ArrayList<>();
                metadatas.forEach((k, v) -> metadataList.add(new Metadata(k, v)));
                metadatasObj.setMetadata(metadataList);
            }
            
            // Extraire le ProcessContext
            ProcessContext processContext = customProcessContext != null ?
                customProcessContext :
                helper.extractProcessContext(exchange);
            
            // Créer l'objet Error final
            Error error = new Error();
            error.setProcessContext(processContext);
            error.setErrorReport(errorReport);
            
            // Ajouter data principal si défini
            if (mainData != null) {
                AnyData anyData = new AnyData(mainData);
                error.setData(anyData);
            }
            
            // Ajouter les références si définies
            if (!references.isEmpty()) {
                references.forEach(ref -> error.getReference().add(ref));
            }
            
            // Ajouter les métadonnées
            if (metadatasObj != null) {
                error.setMetadatas(metadatasObj);
            }
            
            // Envoyer
            helper.sendError(error);
            
            // Auditer automatiquement si activé
            helper.auditErrorIfEnabled(exchange, error, skipAudit);
        }
    }
}
