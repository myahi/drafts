package com.mycompany.transco.model;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "name", "value" })
public class Output {

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "value")
    private String value; // minOccurs=0

    public Output() {}

    public Output(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
