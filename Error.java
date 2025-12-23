package com.mycompany.model;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle JAXB "fait main" pour le XSD racine:
 *
 * <error>
 *   processContext (pfx:ProcessContext)
 *   errorReport    (pfx:ErrorReport)
 *   data           (pfx:anydata) [0..1]
 *   reference      [0..n] (code, codifier)
 *   ns2:metadatas  [0..1]
 * </error>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "error")
@XmlType(name = "", propOrder = {
    "processContext",
    "errorReport",
    "data",
    "reference",
    "metadatas"
})
public class Error {

    // --- éléments unqualified (pas de namespace) ---
    @XmlElement(name = "processContext", required = true)
    private ProcessContext processContext;

    @XmlElement(name = "errorReport", required = true)
    private ErrorReport errorReport;

    @XmlElement(name = "data")
    private AnyData data;

    @XmlElement(name = "reference")
    private List<Reference> reference;

    // --- élément qualifié ns2:metadatas ---
    // namespace = celui de auditinfo.xsd
    @XmlElement(name = "metadatas", namespace = Namespaces.NS2_AUDITINFO)
    private Metadatas metadatas;

    public Error() {
        this.reference = new ArrayList<>();
    }

    // Getters/Setters
    public ProcessContext getProcessContext() { return processContext; }
    public void setProcessContext(ProcessContext processContext) { this.processContext = processContext; }

    public ErrorReport getErrorReport() { return errorReport; }
    public void setErrorReport(ErrorReport errorReport) { this.errorReport = errorReport; }

    public AnyData getData() { return data; }
    public void setData(AnyData data) { this.data = data; }

    public List<Reference> getReference() {
        if (reference == null) reference = new ArrayList<>();
        return reference;
    }
    public void setReference(List<Reference> reference) { this.reference = reference; }

    public Metadatas getMetadatas() { return metadatas; }
    public void setMetadatas(Metadatas metadatas) { this.metadatas = metadatas; }

    // ---------------------------------------------------------------------
    // Types du processinfo.xsd (mais utilisés ici "unqualified")
    // ---------------------------------------------------------------------

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "ProcessContext", propOrder = {
        "processId",
        "projectName",
        "engineName",
        "restartedFromCheckpoint",
        "trackingInfo",
        "customId"
    })
    public static class ProcessContext {

        @XmlElement(name = "ProcessId", required = true)
        private long processId;

        @XmlElement(name = "ProjectName", required = true)
        private String projectName;

        @XmlElement(name = "EngineName", required = true)
        private String engineName;

        @XmlElement(name = "RestartedFromCheckpoint", required = true)
        private boolean restartedFromCheckpoint;

        @XmlElement(name = "TrackingInfo")
        private List<String> trackingInfo;

        @XmlElement(name = "CustomId")
        private String customId;

        public ProcessContext() {
            this.trackingInfo = new ArrayList<>();
        }

        public long getProcessId() { return processId; }
        public void setProcessId(long processId) { this.processId = processId; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public String getEngineName() { return engineName; }
        public void setEngineName(String engineName) { this.engineName = engineName; }

        public boolean isRestartedFromCheckpoint() { return restartedFromCheckpoint; }
        public void setRestartedFromCheckpoint(boolean restartedFromCheckpoint) { this.restartedFromCheckpoint = restartedFromCheckpoint; }

        public List<String> getTrackingInfo() {
            if (trackingInfo == null) trackingInfo = new ArrayList<>();
            return trackingInfo;
        }
        public void setTrackingInfo(List<String> trackingInfo) { this.trackingInfo = trackingInfo; }

        public String getCustomId() { return customId; }
        public void setCustomId(String customId) { this.customId = customId; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "ErrorReport", propOrder = {
        "stackTrace",
        "msg",
        "fullClass",
        "clazz",
        "processStack",
        "msgCode",
        "data"
    })
    public static class ErrorReport {

        @XmlElement(name = "StackTrace", required = true)
        private String stackTrace;

        @XmlElement(name = "Msg", required = true)
        private String msg;

        @XmlElement(name = "FullClass", required = true)
        private String fullClass;

        // "Class" est un mot réservé en Java -> on mappe sur "clazz"
        @XmlElement(name = "Class", required = true)
        private String clazz;

        @XmlElement(name = "ProcessStack", required = true)
        private String processStack;

        @XmlElement(name = "MsgCode")
        private String msgCode;

        @XmlElement(name = "Data")
        private AnyData data;

        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }

        public String getFullClass() { return fullClass; }
        public void setFullClass(String fullClass) { this.fullClass = fullClass; }

        public String getClazz() { return clazz; }
        public void setClazz(String clazz) { this.clazz = clazz; }

        public String getProcessStack() { return processStack; }
        public void setProcessStack(String processStack) { this.processStack = processStack; }

        public String getMsgCode() { return msgCode; }
        public void setMsgCode(String msgCode) { this.msgCode = msgCode; }

        public AnyData getData() { return data; }
        public void setData(AnyData data) { this.data = data; }
    }

    /**
     * anydata = sequence(any)
     * On stocke un Element DOM. Tu peux y mettre <details>...</details> etc.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "anydata", propOrder = {"any"})
    public static class AnyData {

        // JAXB ne sait pas toujours marshaler directement Element sans aide selon impl.
        // Ici on le garde simple: Element + @XmlAnyElement(lax=true)
        @XmlAnyElement(lax = true)
        private Object any;

        public AnyData() {}

        public AnyData(Object any) { this.any = any; }

        public Object getAny() { return any; }
        public void setAny(Object any) { this.any = any; }
    }

    // ---------------------------------------------------------------------
    // reference du schéma racine (code, codifier) - unqualified
    // ---------------------------------------------------------------------

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"code", "codifier"})
    public static class Reference {

        @XmlElement(name = "code", required = true)
        private String code;

        @XmlElement(name = "codifier", required = true)
        private String codifier;

        public Reference() {}
        public Reference(String code, String codifier) {
            this.code = code;
            this.codifier = codifier;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getCodifier() { return codifier; }
        public void setCodifier(String codifier) { this.codifier = codifier; }
    }

    // ---------------------------------------------------------------------
    // auditinfo.xsd : metadatas -> metadata*(key,value) (namespace ns2)
    // ---------------------------------------------------------------------

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "Metadatas", namespace = Namespaces.NS2_AUDITINFO, propOrder = {"metadata"})
    public static class Metadatas {

        @XmlElement(name = "metadata", namespace = Namespaces.NS2_AUDITINFO)
        private List<Metadata> metadata;

        public Metadatas() {
            this.metadata = new ArrayList<>();
        }

        public List<Metadata> getMetadata() {
            if (metadata == null) metadata = new ArrayList<>();
            return metadata;
        }

        public void setMetadata(List<Metadata> metadata) { this.metadata = metadata; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "Metadata", namespace = Namespaces.NS2_AUDITINFO, propOrder = {"key", "value"})
    public static class Metadata {

        @XmlElement(name = "key", namespace = Namespaces.NS2_AUDITINFO, required = true)
        private String key;

        @XmlElement(name = "value", namespace = Namespaces.NS2_AUDITINFO, required = true)
        private String value;

        public Metadata() {}
        public Metadata(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    // ---------------------------------------------------------------------
    // Constantes de namespaces
    // ---------------------------------------------------------------------
    public static final class Namespaces {
        private Namespaces() {}
        public static final String NS2_AUDITINFO =
            "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd";
    }
}
