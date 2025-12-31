package fr.lbp.jms.connection;

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
		safeCloseConnection();
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
	
	@Override
	public void onMessage(Message msg) {
		String msgBody = null;
		try {
			msgBody = msg.getBody(String.class);
			LOGGER.info("Outgoing message received >>> " + msgBody);
			handelIncomingMessage(msg,msgBody);
		}
		 catch (ErrorCode e) {
			 LOGGER.error("Error when client handle message >>> ", e);
			 //this.sendMessage(this.emsServerURL,this.emsUserName,this.emsUserPassword,this.destinationQueueError,Tools.buildErrorMessage(Tools.extractStackTraceExcetpionInoString(e), msgBody));
			 //LOGGER.info("sending message to queue: " + destinationQueueError);
			}
		catch (JMSException e) {
			LOGGER.error("Error when client ack message >>> ", e);
			this.sendMessage(this.emsServerURL,this.emsUserName,this.emsUserPassword,this.destinationQueueError,Tools.buildErrorMessage(Tools.extractStackTraceExcetpionInoString(e), msgBody));
			LOGGER.info("sending message to queue: " + destinationQueueError);
		}
	}
	
	public void senTechnicalErrorMessage(String messageBody){
		this.sendMessage(this.emsServerURL,this.emsUserName,this.emsUserPassword,this.destinationQueueTechnicalError,messageBody);
		LOGGER.info("Technical error has been send to " + this.destinationQueueTechnicalError + " queue");
	}
	public void senFunctionnalErrorMessage(String messageBody){
		this.sendMessage(this.emsServerURL,this.emsUserName,this.emsUserPassword,this.destinationQueueError,messageBody);
	}
	public void senFunctionnalMessage(String messageBody){
		this.sendMessage(this.emsServerURL,this.emsUserName,this.emsUserPassword,this.destinationQueue,messageBody);
	}
    private void sendMessage(String serverUrl, String userName, String password, String queueName,String message){
    	try 
        {
    	ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(serverUrl);
    	Connection connection = factory.createConnection(userName,password);
        Session session = connection.createSession(javax.jms.Session.CLIENT_ACKNOWLEDGE);
        Queue destination = session.createQueue(queueName);
        MessageProducer msgProducer = session.createProducer(null);
    	TextMessage msg;
			msg = session.createTextMessage();
			msg.setText(message);
	    	msgProducer.send(destination, msg);
	    	msgProducer.close();
	    	LOGGER.info("SWMLmessage sent (" + queueName + ") >>> " + message);
		} catch (JMSException e) {
			LOGGER.error(e.getMessage());
		}
    }
	private void handelIncomingMessage(Message msg,String messageContent) throws JMSException, ErrorCode{
		String inputMessageReference = null;
			try {
				//String messageContent = msg.getBody(String.class);
				String markitAction = Tools.getMarkitActionType(messageContent);
				LOGGER.info("MARKIT_DEALER ACTION >>> "  + markitAction);
				LOGGER.info("messageContent >>> "  + messageContent);
				inputMessageReference = Tools.getSourceRefMessage(messageContent);
				String swdml = Tools.getSWDML(messageContent);
				String privateData = Tools.getPrivateData(messageContent);
				String recipientData = Tools.getRecipienData(messageContent);
				String sinkUpdateData = Tools.getSinkUpdateData(messageContent);
				String messageTextData = Tools.getMessageText(messageContent);
				String oldDealVersionHandle = Tools.getOldDealVersionHandle(messageContent);
				String currentDate = Tools.getCurrentTimeStamp();

				if(Tools.MARKIT_ACTION_NEW.equals(markitAction)){
					LOGGER.info("SubmitNewDeal for incoming lbp ref >>> "  + inputMessageReference);
					String dhv = LbpMarkitClient.submitNewDeal(swdml,privateData,recipientData,messageTextData);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_DRAFT_NEW.equals(markitAction)){
					LOGGER.info("SubmitDraftNewDeal for incoming lbp ref >>> "  + inputMessageReference);
					String dhv = LbpMarkitClient.submitDraftNewDeal(swdml,privateData,recipientData,messageTextData);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
					String outgoing = Tools.buildReponseToSourceForDraft(inputMessageReference,dhv);
					sendMessage(this.emsServerURL, this.emsUserName, this.emsUserPassword, this.destinationQueue, outgoing);
				}
				else if(Tools.MARKIT_ACTION_DRAFT_AND_TRANSFERT.equals(markitAction)){
					LOGGER.info("SubmitDraftNewDealAndTransfert for incoming lbp ref >>> "  + inputMessageReference);
					String dhv = LbpMarkitClient.submitDraftNewDeal(swdml,privateData,recipientData,messageTextData);
					LOGGER.info("recipientDataOne ===> " + recipientData);
					String transferRecipientXML = Tools.getSecondRecipienData(messageContent);;
					LOGGER.info("recipientDataTwo ===> " + transferRecipientXML);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
					String newDNH = LbpMarkitClient.transferDeal(dhv, privateData, transferRecipientXML, "TRANSFERT DRAFT FROM API USER TO MARKIT USER");
				}
				else if(Tools.MARKIT_ACTION_RELEASE.equals(markitAction)){
					LOGGER.info("ReleaseDeal for incoming lbp ref >>> "  + inputMessageReference);
					String dhv = LbpMarkitClient.releaseDeal(privateData, oldDealVersionHandle);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_PICKUP.equals(markitAction)){
					LOGGER.info("PickUpDeal for incoming lbp ref >>> "  + inputMessageReference);
					String dhv = LbpMarkitClient.pickUpDeal(oldDealVersionHandle, privateData);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_AFFIRM.equals(markitAction)){
					swdml = Tools.getSWDMLForAffirm(messageContent);
					LOGGER.info("AffirmDeal for incoming lbp ref >>> "  + inputMessageReference);
					LOGGER.info("AffirmDeal for incoming lbp ref with swdml >>> "  + swdml);
					String dhv = LbpMarkitClient.affirmDeal(oldDealVersionHandle,privateData,swdml);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_WITHDRAW.equals(markitAction)){
					LOGGER.info("WithdrawDeal for incoming lbp ref >>> "  + inputMessageReference);
					String dhv = LbpMarkitClient.withdrawDeal(oldDealVersionHandle,privateData);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_ACCEPT_AFFIRM.equals(markitAction)){
					LOGGER.info("AcceptAffirmDeal for incoming lbp ref >>> "  + inputMessageReference);
					String dhv = LbpMarkitClient.acceptAffirmDeal(oldDealVersionHandle, privateData);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_CANCELLATION.equals(markitAction)){
					LOGGER.info("Cancellation for incoming lbp ref >>> "  + inputMessageReference);
					String postTradeXML = Tools.getCancellationPart(messageContent);
					String dhv = LbpMarkitClient.submitPostTradeEvent(privateData, oldDealVersionHandle, postTradeXML, recipientData, messageTextData);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_PARTIAL_TERMANATE.equals(markitAction)){
					LOGGER.info("Partial terminate for incoming lbp ref >>> "  + inputMessageReference);
					String postTradeXML = Tools.getPartialTerminatePart(messageContent);
					String dhv = LbpMarkitClient.submitPostTradeEvent(privateData, oldDealVersionHandle, postTradeXML, recipientData, messageTextData);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_EXERCISE.equals(markitAction)){
					LOGGER.info("Exercise for incoming lbp ref >>> "  + inputMessageReference);
					String postTradeXML = Tools.getExercisePart(messageContent);
					String dhv = LbpMarkitClient.submitPostTradeEvent(privateData, oldDealVersionHandle, postTradeXML, recipientData, messageTextData);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_REJECT.equals(markitAction)){
					LOGGER.info("Reject for incoming lbp ref >>> "  + inputMessageReference);
					String dhv = LbpMarkitClient.rejectDeal(oldDealVersionHandle, messageTextData);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_TRANSFER.equals(markitAction)){
					LOGGER.info("Transfer for incoming lbp ref >>> "  + inputMessageReference);
					String transferRecipientXML = recipientData;
					LOGGER.info("recipientDataOne ===> " + recipientData);
					String dhv = LbpMarkitClient.transferDeal(oldDealVersionHandle, privateData, transferRecipientXML, messageTextData);
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else if(Tools.MARKIT_ACTION_UPDATE.equals(markitAction)){
					LOGGER.info("Update for incoming lbp ref >>> "  + inputMessageReference);
					String dhv = LbpMarkitClient.updateDeal(oldDealVersionHandle, sinkUpdateData);
					
					Tools.updateLastExchangeTimeStamp(currentDate);
					LOGGER.info("Markit DVH message received >>> " + dhv + " for incoming message " + inputMessageReference);
				}
				else {
					LOGGER.error("Unhandlend action Markit for incoming lbp ref >>> "  + inputMessageReference);
				}
				msg.acknowledge();
			} catch (IOException | JMSException | ErrorCode e ) {
				LOGGER.error(e);
				String outgoing;
				if(e instanceof ErrorCode){
					int errorCode = ((ErrorCode) e).errorCode;
					if(isConnectionError(errorCode)){
						sendMessage(this.emsServerURL, this.emsUserName, this.emsUserPassword, this.destinationQueueTechnicalError, e.getMessage());
						//this.lbpMarkitClient.stopMarkitConnector();
						//this.lbpMarkitClient.initializeMarkitConnector();
					}
					else {
						outgoing = Tools.buildErrorToSource(inputMessageReference, String.valueOf(errorCode), e.toString());
						sendMessage(this.emsServerURL, this.emsUserName, this.emsUserPassword, this.destinationQueueError, outgoing);
						msg.acknowledge();
					}
					outgoing = Tools.buildErrorToSource(inputMessageReference, String.valueOf(errorCode), e.toString());
				}
				else {
					outgoing = Tools.buildErrorToSource(inputMessageReference, "", e.toString());
					sendMessage(this.emsServerURL, this.emsUserName, this.emsUserPassword, this.destinationQueueError, outgoing);
					msg.acknowledge();
				}
				}
	}
	private boolean isConnectionError(int errorCode){
		return errorCode == SWAPILinkModuleConstants.SWERR_InvalidHandle || 
				errorCode == SWAPILinkModuleConstants.SWERR_LostConnection ||
				errorCode == SWAPILinkModuleConstants.SWERR_UserLoggedOut ||
				errorCode == SWAPILinkModuleConstants.SWERR_PasswordExpired || 
				errorCode == SWAPILinkModuleConstants.SWERR_LoginLimitReached ||
				errorCode == SWAPILinkModuleConstants.SWERR_AccountLocked ||
				errorCode == SWAPILinkModuleConstants.SWERR_Timeout;
	}

	public void handNotifyCallback(SW_DealNotifyData dnData) {
		try {
//			String currentDate = Tools.getCurrentTimeStamp();
//			Tools.updateLastExchangeTimeStamp(currentDate);
			String swml = LbpMarkitClient.getDealSWML(dnData.getDvh());
			String outgoing = Tools.buildResponseToSource(dnData,swml);
			sendMessage(this.emsServerURL, this.emsUserName, this.emsUserPassword, this.destinationQueue, outgoing);
		} catch (ErrorCode e) {
			e.printStackTrace();
		}
	}

	public LbpMarkitClient getLbpMarkitClient() {
		return lbpMarkitClient;
	}

	public void setLbpMarkitClient(LbpMarkitClient lbpMarkitClient) {
		this.lbpMarkitClient = lbpMarkitClient;
	}
	
   }
