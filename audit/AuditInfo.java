package fr.lbp.lib.model.auditinfo;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = { "timestamp", "reference", "metadatas", "description", "data", "statut" }
)
@XmlRootElement(
    name = "auditInfo",
    namespace = "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd"
)
public class AuditInfo {

    @XmlElement(
        name = "timestamp",
        namespace = "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd"
    )
    private String timestamp; // minOccurs=0

    @XmlElement(
        name = "reference",
        namespace = "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd"
    )
    private List<Reference> reference; // minOccurs=0 maxOccurs=unbounded

    @XmlElement(
        name = "metadatas",
        namespace = "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd"
    )
    private Metadatas metadatas; // minOccurs=0

    @XmlElement(
        name = "description",
        namespace = "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd",
        required = true
    )
    private String description;

    @XmlElement(
        name = "data",
        namespace = "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd",
        required = true
    )
    private String data;

    @XmlElement(
        name = "statut",
        namespace = "http://www.tibco.com/schemas/EAIFramework/framework/resources/XSD/auditInfo.xsd"
    )
    private String statut; // minOccurs=0

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<Reference> getReference() {
        if (reference == null) {
            reference = new ArrayList<>();
        }
        return reference;
    }

    public void setReference(List<Reference> reference) {
        this.reference = reference;
    }

    public Metadatas getMetadatas() {
        return metadatas;
    }

    public void setMetadatas(Metadatas metadatas) {
        this.metadatas = metadatas;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
