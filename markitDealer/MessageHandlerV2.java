import javax.jms.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.lbp.markit.configuration.ApplicationProperties;
import fr.lbp.markit.controller.LbpMarkitClient;

public class MessageHandler implements MessageListener {

    private static final Logger LOGGER = LogManager.getLogger(MessageHandler.class);

    private final String emsServerEnv;
    private final String emsServerURL;
    private final String emsUserName;
    private final String emsUserPassword;

    private final String incomingQueue;
    private final String destinationQueue;
    private final String destinationQueueError;
    private final String destinationQueueTechnicalError;

    private volatile boolean closing = false;
    private volatile boolean started = false;

    private ConnectionFactory factory;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    private LbpMarkitClient lbpMarkitClient;

    public MessageHandler() {
        this.emsServerEnv = ApplicationProperties.LBP_EMS_SERVER_ENV;
        this.emsServerURL = ApplicationProperties.LBP_EMS_SERVER_URL;
        this.emsUserName = ApplicationProperties.LBP_EMS_USER_NAME;
        this.emsUserPassword = ApplicationProperties.LBP_EMS_PASSWORD;

        this.incomingQueue = emsServerEnv + "." + ApplicationProperties.LBP_FROM_BFI_QUEUE_NAME;
        this.destinationQueue = emsServerEnv + "." + ApplicationProperties.LBP_TO_BFI_QUEUE_NAME;
        this.destinationQueueError = emsServerEnv + "." + ApplicationProperties.LBP_TO_BFI_ERROR_QUEUE_NAME;
        this.destinationQueueTechnicalError = emsServerEnv + "." + ApplicationProperties.LBP_TO_BFI_TECHNICAL_ERROR_QUEUE_NAME;

        this.factory = new com.tibco.tibjms.TibjmsConnectionFactory(emsServerURL);
    }

    public void setLbpMarkitClient(LbpMarkitClient client) {
        this.lbpMarkitClient = client;
    }

    public synchronized void startListening() {
        // réarmement
        closing = false;

        if (started) {
            LOGGER.info("Listener already started on queue >>> {}", incomingQueue);
            return;
        }

        // sécurité : nettoyage si restes
        safeClose();

        try {
            connection = factory.createConnection(emsUserName, emsUserPassword);
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            Queue incoming = session.createQueue(incomingQueue);
            consumer = session.createConsumer(incoming);
            consumer.setMessageListener(this);

            connection.start();
            started = true;

            LOGGER.info("Start listening incoming messages on queue >>> {}", incomingQueue);
        } catch (JMSException e) {
            LOGGER.error("Failed to start JMS listener", e);
            safeClose();
            started = false;
        }
    }

    public synchronized void stopListening() {
        if (closing) return;
        closing = true;

        safeClose();
        started = false;

        LOGGER.info("Stop listening incoming messages on queue >>> {}", incomingQueue);
    }

    private void safeClose() {
        // fermer dans l'ordre consumer -> session -> connection
        try {
            if (consumer != null) {
                try { consumer.close(); } catch (Exception ignore) {}
            }
        } finally {
            consumer = null;
        }

        try {
            if (session != null) {
                try { session.close(); } catch (Exception ignore) {}
            }
        } finally {
            session = null;
        }

        try {
            if (connection != null) {
                try {
                    try { connection.stop(); } catch (javax.jms.IllegalStateException ignore) {}
                    connection.close();
                } catch (Exception ignore) {}
            }
        } finally {
            connection = null;
        }
    }

    @Override
    public void onMessage(Message message) {
        // ton code existant
    }
}
