package com.mycompany.model;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"code", "codifier"})
public class Reference {

    @XmlElement(required = true)
    private String code;

    @XmlElement(required = true)
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
