package com.mycompany.model;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "error")
@XmlType(propOrder = {
    "processContext",
    "errorReport",
    "data",
    "reference",
    "metadatas"
})
public class Error {

    @XmlElement(name = "processContext", required = true)
    private ProcessContext processContext;

    @XmlElement(name = "errorReport", required = true)
    private ErrorReport errorReport;

    @XmlElement(name = "data")
    private AnyData data;

    @XmlElement(name = "reference")
    private List<Reference> reference = new ArrayList<>();

    @XmlElement(
        name = "metadatas",
        namespace = Namespaces.AUDITINFO
    )
    private Metadatas metadatas;

    // getters / setters
    public ProcessContext getProcessContext() { return processContext; }
    public void setProcessContext(ProcessContext processContext) { this.processContext = processContext; }

    public ErrorReport getErrorReport() { return errorReport; }
    public void setErrorReport(ErrorReport errorReport) { this.errorReport = errorReport; }

    public AnyData getData() { return data; }
    public void setData(AnyData data) { this.data = data; }

    public List<Reference> getReference() { return reference; }

    public Metadatas getMetadatas() { return metadatas; }
    public void setMetadatas(Metadatas metadatas) { this.metadatas = metadatas; }
}
