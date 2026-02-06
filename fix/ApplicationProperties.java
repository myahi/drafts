package fr.labanquepostale.fix.connection.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lbp")
public class ApplicationProperties {

    // lbp.ems.serverUrl
    private String lbpEmsServerUrl;

    // lbp.ems.serverEnv
    private String lbpEmsServerEnv;

    // lbp.ems.password
    private String lbpEmsPassword;

    // lbp.ems.queueName
    private String lbpEmsQueueName;

    // lbp.ems.error.queueName
    private String lbpEmsErrorQueueName;

    // lbp.ems.sendIncomingMessage
    private boolean lbpEmsSendIncomingMessage;
    
    // lbp.ems.userName
    private String lbpEmsUserName;

    // lbp.ems.incoming.queueName
    private String lbpEmsIncomingQueueName;

    /* ===== CONFIRM ===== */

    // lbp.confirm.message.to.server
    private boolean lbpConfirmMessageToServer;

    // lbp.dico.path
    private String lbpDicoPath;
    
 // lbp.incoming.notification.folder.path
    private String lbpIncomingNotificationFolderPath;
}
