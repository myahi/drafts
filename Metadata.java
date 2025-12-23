package com.mycompany.model;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "Metadata",
    namespace = Namespaces.AUDITINFO,
    propOrder = {"key", "value"}
)
public class Metadata {

    @XmlElement(namespace = Namespaces.AUDITINFO, required = true)
    private String key;

    @XmlElement(namespace = Namespaces.AUDITINFO, required = true)
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
