package fr.lbp.jms.connection;

import javax.jms.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tibco.tibjms.TibjmsConnectionFactory;

public class EmsJmsListener implements MessageListener {

    private static final Logger LOGGER = LogManager.getLogger(EmsJmsListener.class);

    public enum OutgoingType {
        FUNCTIONAL,
        FUNCTIONAL_ERROR,
        TECHNICAL_ERROR
    }

    private final String serverUrl;
    private final String username;
    private final String password;

    private final String incomingQueueName;

    // Queues de sortie (connues uniquement ici)
    private final String destinationQueue;
    private final String destinationQueueError;
    private final String destinationQueueTechnicalError;

    private final MarkitIncomingService incomingService;

    private volatile boolean closing = false;
    private volatile boolean started = false;

    private ConnectionFactory factory;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    public EmsJmsListener(String serverUrl,
                          String username,
                          String password,
                          String incomingQueueName,
                          String destinationQueue,
                          String destinationQueueError,
                          String destinationQueueTechnicalError,
                          MarkitIncomingService incomingService) {

        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;

        this.incomingQueueName = incomingQueueName;

        this.destinationQueue = destinationQueue;
        this.destinationQueueError = destinationQueueError;
        this.destinationQueueTechnicalError = destinationQueueTechnicalError;

        this.incomingService = incomingService;

        this.factory = new TibjmsConnectionFactory(serverUrl);
    }

    public synchronized void startListening() {
        closing = false;

        if (started) {
            LOGGER.info("Listener already started on queue >>> {}", incomingQueueName);
            return;
        }

        safeClose();

        try {
            connection = factory.createConnection(username, password);
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            Queue incoming = session.createQueue(incomingQueueName);
            consumer = session.createConsumer(incoming);
            consumer.setMessageListener(this);

            connection.start();
            started = true;

            LOGGER.info("Start listening incoming messages on queue >>> {}", incomingQueueName);
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

        LOGGER.info("Stop listening incoming messages on queue >>> {}", incomingQueueName);
    }

    @Override
    public void onMessage(Message msg) {
        String body = null;

        try {
            body = msg.getBody(String.class);
            LOGGER.info("Incoming message received >>> {}", body);

            // Délégation au métier (qui ne connaît pas les queues)
            incomingService.handle(body);

            // ACK uniquement si OK
            msg.acknowledge();

        } catch (Exception e) {
            LOGGER.error("Error while processing incoming message", e);

            // Choix: ACK ou pas ACK en erreur ?
            // - Si tu ACK => pas de redelivery
            // - Si tu n'ACK pas => redelivery possible (selon broker)
            //
            // Pour rester proche du comportement “sécurisant” (pas de boucle),
            // tu peux ACK même en erreur :
            //
            // try { msg.acknowledge(); } catch (JMSException ignore) {}
        }
    }

    /**
     * Le métier appelle ça avec un type sémantique.
     * Ici on route vers la bonne queue.
     */
    public void send(OutgoingType type, String payload) {
        String queueName;

        switch (type) {
            case FUNCTIONAL:
                queueName = destinationQueue;
                break;
            case FUNCTIONAL_ERROR:
                queueName = destinationQueueError;
                break;
            case TECHNICAL_ERROR:
                queueName = destinationQueueTechnicalError;
                break;
            default:
                throw new IllegalArgumentException("Unknown outgoing type: " + type);
        }

        sendToQueue(queueName, payload);
    }

    private void sendToQueue(String queueName, String payload) {
        Connection c = null;
        Session s = null;
        MessageProducer p = null;

        try {
            ConnectionFactory f = new TibjmsConnectionFactory(serverUrl);
            c = f.createConnection(username, password);
            s = c.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            Queue q = s.createQueue(queueName);
            p = s.createProducer(q);

            TextMessage tm = s.createTextMessage(payload);
            p.send(tm);

            LOGGER.info("Message sent ({}) >>> {}", queueName, payload);

        } catch (JMSException e) {
            LOGGER.error("Failed to send message to queue " + queueName, e);
        } finally {
            try { if (p != null) p.close(); } catch (Exception ignore) {}
            try { if (s != null) s.close(); } catch (Exception ignore) {}
            try { if (c != null) c.close(); } catch (Exception ignore) {}
        }
    }

    private void safeClose() {
        try { if (consumer != null) consumer.close(); } catch (Exception ignore) {}
        consumer = null;

        try { if (session != null) session.close(); } catch (Exception ignore) {}
        session = null;

        try {
            if (connection != null) {
                try { connection.stop(); } catch (IllegalStateException ignore) {}
                connection.close();
            }
        } catch (Exception ignore) {}
        connection = null;
    }
}
