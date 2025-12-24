package fr.lbp.lib.model.audit;

import jakarta.xml.bind.annotation.*;

import fr.lbp.lib.model.auditinfo.AuditInfo;
import fr.lbp.lib.model.enginetypes.ProcessContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "auditInfo", "processContext", "forceAudit" })
@XmlRootElement(
    name = "audit",
    namespace = "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd"
)
public class Audit {

    @XmlElement(
        name = "auditInfo",
        namespace = "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd",
        required = true
    )
    private AuditInfo auditInfo;

    @XmlElement(
        name = "ProcessContext",
        namespace = "http://www.tibco.com/pe/EngineTypes",
        required = true
    )
    private ProcessContext processContext;

    @XmlElement(
        name = "forceAudit",
        namespace = "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd"
    )
    private Boolean forceAudit; // minOccurs=0

    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    public void setAuditInfo(AuditInfo auditInfo) {
        this.auditInfo = auditInfo;
    }

    public ProcessContext getProcessContext() {
        return processContext;
    }

    public void setProcessContext(ProcessContext processContext) {
        this.processContext = processContext;
    }

    public Boolean getForceAudit() {
        return forceAudit;
    }

    public void setForceAudit(Boolean forceAudit) {
        this.forceAudit = forceAudit;
    }
}
