package com.mycompany.model;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
    "stackTrace",
    "msg",
    "fullClass",
    "clazz",
    "processStack",
    "msgCode",
    "data"
})
public class ErrorReport {

    @XmlElement(name = "StackTrace", required = true)
    private String stackTrace;

    @XmlElement(name = "Msg", required = true)
    private String msg;

    @XmlElement(name = "FullClass", required = true)
    private String fullClass;

    // "Class" est un mot réservé Java
    @XmlElement(name = "Class", required = true)
    private String clazz;

    @XmlElement(name = "ProcessStack", required = true)
    private String processStack;

    @XmlElement(name = "MsgCode")
    private String msgCode;

    @XmlElement(name = "Data")
    private AnyData data;

    // getters / setters
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
