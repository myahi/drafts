package fr.lbp.lib.error;

import com.mycompany.model.*;
import fr.lbp.lib.audit.TibcoAuditHelper;
import fr.lbp.lib.ems.EMSSenderHelper;
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
public class TibcoErrorHelper {
    
    @Autowired
    private ProducerTemplate producerTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private TibcoAuditHelper auditHelper;
    
    @Autowired(required = false)
    private EMSSenderHelper emsSender;
    
    @Value("${error.auto-audit.enabled:true}")
    private boolean autoAuditEnabled;
    
    @Value("${error.logging.enabled:true}")
    private boolean errorLoggingEnabled;
    
    @Value("${error.logging.level:ERROR}")
    private String errorLoggingLevel;
    
    @Value("${error.logging.include-stacktrace:true}")
    private boolean includeStackTrace;
    
    @Value("${error.destination:seda}")
    private String errorDestination;
    
    /**
     * Point d'entrée principal
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
     * Crée un ErrorBuilder à partir de l'exception dans l'exchange
     */
    public ErrorBuilder errorFromExchange(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        if (exception == null) {
            exception = exchange.getException();
        }
        return error(exchange, exception);
    }
    
    /**
     * Envoie l'erreur vers la destination configurée
     */
    public void sendError(Error error) {
        try {
            // Choisir la destination selon la config
            if ("ems".equalsIgnoreCase(errorDestination) && emsSender != null) {
                emsSender.sendError(error);
                log.debug("Error sent to EMS: msg={}, class={}", 
                    error.getErrorReport().getMsg(), 
                    error.getErrorReport().getClazz());
            } else {
                producerTemplate.asyncSendBody("seda:error", error);
                log.debug("Error sent to SEDA: msg={}, class={}", 
                    error.getErrorReport().getMsg(), 
                    error.getErrorReport().getClazz());
            }
            
            // Logger conditionnellement
            if (errorLoggingEnabled) {
                logError(error);
            }
            
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send error to queue - msg={}, class={}", 
                error.getErrorReport().getMsg(), 
                error.getErrorReport().getClazz(), e);
        }
    }
    
    /**
     * Log l'erreur selon le niveau configuré
     */
    private void logError(Error error) {
        ErrorReport report = error.getErrorReport();
        
        String logMessage = String.format("Error: msg=%s, class=%s, msgCode=%s, correlationId=%s",
            report.getMsg(),
            report.getClazz(),
            report.getMsgCode(),
            error.getProcessContext().getCustomId());
        
        if ("ERROR".equalsIgnoreCase(errorLoggingLevel)) {
            if (includeStackTrace) {
                log.error("{}\nStackTrace: {}", logMessage, report.getStackTrace());
            } else {
                log.error(logMessage);
            }
        } else if ("WARN".equalsIgnoreCase(errorLoggingLevel)) {
            log.warn(logMessage);
        } else {
            log.error(logMessage);
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
                .data(errorReport.getMsg())
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
    
    private ProcessContext extractProcessContext(Exchange exchange) {
        ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
        
        if (ctx == null) {
            ctx = createDefaultProcessContext(exchange);
            exchange.setProperty("processContext", ctx);
        }
        
        return ctx;
    }
    
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
        private boolean skipAudit = false;
        
        ErrorBuilder(Exchange exchange, TibcoErrorHelper helper) {
            this.exchange = exchange;
            this.helper = helper;
        }
        
        public ErrorBuilder fromException(Exception exception) {
            this.stackTrace = helper.getStackTraceAsString(exception);
            this.msg = exception.getMessage() != null ? exception.getMessage() : "No message";
            this.fullClass = exception.getClass().getName();
            this.clazz = exception.getClass().getSimpleName();
            
            this.processStack = exchange.getFromRouteId() + " -> " + 
                (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown");
            
            if (exception.getCause() != null) {
                this.metadatas.put("causeClass", exception.getCause().getClass().getName());
                this.metadatas.put("causeMessage", exception.getCause().getMessage());
            }
            
            return this;
        }
        
        public ErrorBuilder message(String msg) {
            this.msg = msg;
            return this;
        }
        
        public ErrorBuilder msg(String msg) {
            return message(msg);
        }
        
        public ErrorBuilder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }
        
        public ErrorBuilder fullClass(String fullClass) {
            this.fullClass = fullClass;
            return this;
        }
        
        public ErrorBuilder clazz(String clazz) {
            this.clazz = clazz;
            return this;
        }
        
        public ErrorBuilder processStack(String processStack) {
            this.processStack = processStack;
            return this;
        }
        
        public ErrorBuilder msgCode(String msgCode) {
            this.msgCode = msgCode;
            return this;
        }
        
        public ErrorBuilder errorData(Object data) {
            this.errorData = data;
            return this;
        }
        
        public ErrorBuilder data(Object data) {
            this.mainData = data;
            return this;
        }
        
        public ErrorBuilder reference(String code, String codifier) {
            Reference ref = new Reference(code, codifier);
            this.references.add(ref);
            return this;
        }
        
        public ErrorBuilder ref(String code, String codifier) {
            return reference(code, codifier);
        }
        
        public ErrorBuilder metadata(String key, String value) {
            this.metadatas.put(key, value);
            return this;
        }
        
        public ErrorBuilder meta(String key, String value) {
            return metadata(key, value);
        }
        
        public ErrorBuilder meta(String key, Object value) {
            this.metadatas.put(key, value != null ? value.toString() : null);
            return this;
        }
        
        public ErrorBuilder metas(Map<String, Object> additionalMetadata) {
            if (additionalMetadata != null) {
                additionalMetadata.forEach((k, v) -> 
                    this.metadatas.put(k, v != null ? v.toString() : null));
            }
            return this;
        }
        
        public ErrorBuilder processContext(ProcessContext ctx) {
            this.customProcessContext = ctx;
            return this;
        }
        
        public ErrorBuilder tracking(String trackingInfo) {
            ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
            if (ctx == null) {
                ctx = helper.createDefaultProcessContext(exchange);
                exchange.setProperty("processContext", ctx);
            }
            ctx.getTrackingInfo().add(trackingInfo);
            return this;
        }
        
        public ErrorBuilder skipAudit() {
            this.skipAudit = true;
            return this;
        }
        
        public ErrorBuilder withAudit() {
            this.skipAudit = false;
            return this;
        }
        
        public void send() {
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
            
            ErrorReport errorReport = new ErrorReport();
            errorReport.setStackTrace(stackTrace);
            errorReport.setMsg(msg);
            errorReport.setFullClass(fullClass);
            errorReport.setClazz(clazz);
            errorReport.setProcessStack(processStack);
            errorReport.setMsgCode(msgCode);
            
            if (errorData != null) {
                AnyData anyData = new AnyData(errorData);
                errorReport.setData(anyData);
            }
            
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
            
            Metadatas metadatasObj = null;
            if (!metadatas.isEmpty()) {
                metadatasObj = new Metadatas();
                List<Metadata> metadataList = new ArrayList<>();
                metadatas.forEach((k, v) -> metadataList.add(new Metadata(k, v)));
                metadatasObj.setMetadata(metadataList);
            }
            
            ProcessContext processContext = customProcessContext != null ?
                customProcessContext :
                helper.extractProcessContext(exchange);
            
            Error error = new Error();
            error.setProcessContext(processContext);
            error.setErrorReport(errorReport);
            
            if (mainData != null) {
                AnyData anyData = new AnyData(mainData);
                error.setData(anyData);
            }
            
            if (!references.isEmpty()) {
                references.forEach(ref -> error.getReference().add(ref));
            }
            
            if (metadatasObj != null) {
                error.setMetadatas(metadatasObj);
            }
            
            helper.sendError(error);
            helper.auditErrorIfEnabled(exchange, error, skipAudit);
        }
    }
}
