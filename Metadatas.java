package com.mycompany.model;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "Metadatas",
    namespace = Namespaces.AUDITINFO,
    propOrder = {"metadata"}
)
public class Metadatas {

    @XmlElement(name = "metadata", namespace = Namespaces.AUDITINFO)
    private List<Metadata> metadata = new ArrayList<>();

    public List<Metadata> getMetadata() { return metadata; }
}
