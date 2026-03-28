package fr.labanquepostale.eai.ftp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FtpMessage", propOrder = {"eaiFtpMessage"})
@XmlRootElement(name = "FtpMessage", namespace = FtpMessage.REQUEST_NAMESPACE)
public class FtpMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String REQUEST_NAMESPACE = "http://www.labanquepostale.fr/dmf/FtpRequest/request";

    @XmlElement(name = "EAIftpMessage", namespace = REQUEST_NAMESPACE, required = true)
    private EaiFtpMessage eaiFtpMessage;

    public enum Action {
        send,
        get,
        list,
        sendandget,
        sendwithack
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"header", "file"})
    public static class EaiFtpMessage implements Serializable {

        private static final long serialVersionUID = 1L;

        @XmlElement(name = "Header", namespace = REQUEST_NAMESPACE, required = true)
        private Header header;

        @XmlElement(name = "File", namespace = REQUEST_NAMESPACE, required = true)
        private File file;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "nbMaxRetry",
            "waitForRetry",
            "batchSchedulerInfo",
            "batchSchedulerChain"
    })
    public static class Header implements Serializable {

        private static final long serialVersionUID = 1L;

        @XmlElement(name = "nbMaxRetry", namespace = REQUEST_NAMESPACE)
        private String nbMaxRetry;

        @XmlElement(name = "waitForRetry", namespace = REQUEST_NAMESPACE)
        private String waitForRetry;

        @XmlElement(name = "batchSchedulerInfo", namespace = REQUEST_NAMESPACE)
        private Boolean batchSchedulerInfo;

        @XmlElement(name = "batchSchedulerChain", namespace = REQUEST_NAMESPACE)
        private String batchSchedulerChain;

        @XmlAttribute(name = "Action", required = true)
        private Action action;

        @XmlAttribute(name = "ConnectionName", required = true)
        private String connectionName;

        @XmlAttribute(name = "IntervalBeforeGet")
        private Integer intervalBeforeGet;

        @XmlAttribute(name = "Timeout")
        private Integer timeout;

        @XmlAttribute(name = "doNotMailOnError")
        private String doNotMailOnError;

        @XmlAttribute(name = "requestURI")
        private String requestURI;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"content"})
    public static class File implements Serializable {

        private static final long serialVersionUID = 1L;

        @XmlElement(name = "Content", namespace = REQUEST_NAMESPACE)
        private String content;

        @XmlAttribute(name = "Location")
        private String location;

        @XmlAttribute(name = "Name", required = true)
        private String name;

        @XmlAttribute(name = "RemoteLocation")
        private String remoteLocation;
    }
}
