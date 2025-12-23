package com.mycompany.model;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "anydata")
public class AnyData {

    @XmlAnyElement(lax = true)
    private Object any;

    public AnyData() {}
    public AnyData(Object any) { this.any = any; }

    public Object getAny() { return any; }
    public void setAny(Object any) { this.any = any; }
}
