package com.mycompany.transco.model;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "transcos")
@XmlType(propOrder = { "source", "target", "transco" })
public class Transcos {

    @XmlElement(name = "source", required = true)
    private String source;

    @XmlElement(name = "target", required = true)
    private String target;

    @XmlElement(name = "transco")
    private List<Transco> transco = new ArrayList<>();

    public Transcos() {}

    public Transcos(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public List<Transco> getTransco() { return transco; }
}
