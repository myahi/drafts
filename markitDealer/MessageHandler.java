import java.io.IOException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.swapswire.sw_api.SWAPILinkModuleConstants;
import com.swapswire.sw_api.SW_DealNotifyData;

import fr.lbp.markit.configuration.ApplicationProperties;
import fr.lbp.markit.connection.ErrorCode;
import fr.lbp.markit.controller.LbpMarkitClient;
import fr.lbp.markit.tools.Tools;

public class MessageHandler implements MessageListener
   {
	private static final Logger LOGGER = LogManager.getLogger(MessageHandler.class);
	private String emsServerEnv = null;
	private String emsServerURL = null;
	private String emsUserName = null;
	private String emsUserPassword = null;
	private String destinationQueue = null;
	private String incomingQueue = null;
	private String destinationQueueError = null;
	private String destinationQueueTechnicalError = null;
	private Connection connection = null;
	private LbpMarkitClient lbpMarkitClient = null;
	
	private volatile boolean closing = false;
	private volatile boolean started;
	
	
	public MessageHandler() throws JMSException{
		this.emsServerEnv = ApplicationProperties.LBP_EMS_SERVER_ENV;
		this.emsServerURL = ApplicationProperties.LBP_EMS_SERVER_URL;
		this.emsUserName = ApplicationProperties.LBP_EMS_USER_NAME;
		this.emsUserPassword = ApplicationProperties.LBP_EMS_PASSWORD;
		this.incomingQueue = emsServerEnv + "." + ApplicationProperties.LBP_FROM_BFI_QUEUE_NAME;
		this.destinationQueue = emsServerEnv + "." + ApplicationProperties.LBP_TO_BFI_QUEUE_NAME;
		this.destinationQueueError = emsServerEnv + "." + ApplicationProperties.LBP_TO_BFI_ERROR_QUEUE_NAME;
		this.destinationQueueTechnicalError = emsServerEnv + "." + ApplicationProperties.LBP_TO_BFI_TECHNICAL_ERROR_QUEUE_NAME;
		ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(emsServerURL);
    	this.connection = factory.createConnection(emsUserName,emsUserPassword);
        Session session = connection.createSession(javax.jms.Session.CLIENT_ACKNOWLEDGE);
        Queue incoming = session.createQueue(incomingQueue);
        MessageConsumer queueReceiver = session.createConsumer(incoming);
        queueReceiver.setMessageListener(this);
	}

	public void startListening(){
		closing =false;
		if(started) {
			LOGGER.info("Listener already started on queue >>> " + incomingQueue);
			return;
		}
		if(started) {
			safeCloseConnection();			
		}
		try {
			this.connection.start();
			started = true;
			LOGGER.info("Start listenning incoming messages on queue >>> " + incomingQueue);
		} catch (JMSException e) {
			LOGGER.error(e);
		}
	}
	
	private void safeCloseConnection() {
		try {
			connection.stop();
			connection.close();
		} catch (Exception ignore) {
		}
		finally {
			started = false;
		}
	}

	public synchronized void stopListening(){
		if(closing) {
			return;
		}
		closing = true;
		started = false;
		try {
			this.connection.stop();
			this.connection.close();
		} catch (JMSException e) {
			LOGGER.warn("Error while closing JMS connection",e);
		}
		finally {
			LOGGER.info("Stop listenning incoming messages on queue >>> " + incomingQueue);			
		}
	}
	}
