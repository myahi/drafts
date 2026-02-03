package fr.labanquepostale.fix.connection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.labanquepostale.tools.JavaPropertiesSetter;
import fr.labanquepostale.tools.JmsMessageSender;
import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.MessageUtils;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.ExecType;
import quickfix.field.MsgType;
import quickfix.field.TradeReportID;
import quickfix.field.TradeReportRefID;
import quickfix.field.TradeReportTransType;
import quickfix.field.TradeReportType;
import quickfix.field.TrdMatchID;
import quickfix.field.TrdRptStatus;
import quickfix.field.TrdType;
import quickfix.fix50sp2.TradeCaptureReportAck;


public class FixApplication implements Application {
	
	private static final Logger LOGGER = LogManager.getLogger(FixClientStarter.class);
	JmsMessageSender jmsSender = null;
	private String destinationQueue = null;
	private String incomingMsgFolderPath = null;
	private final String FILE_SEPARATOR = System.getProperty("file.separator");
	private final String FILE_EXTENSION = ".txt";
	private boolean pushIncomingMessageToQueue = false;
	private String emsServerEnv = null;
	private String emsServerURL = null;
	private String emsUserName = null;
	private String emsUserPassword = null;
	private String confirmMessageToServer = null;
	
	public FixApplication(String emsPropertiesFile, String incomingMsgFolderPath){
		JavaPropertiesSetter.initializeEMSPrperties(emsPropertiesFile);
		this.pushIncomingMessageToQueue = "true".equals(System.getProperty(JavaPropertiesSetter.EMS_SEND_INCOMING_MESSAGE)) ? true : false;
		this.incomingMsgFolderPath = incomingMsgFolderPath +  FILE_SEPARATOR;
		if (pushIncomingMessageToQueue){
		this.emsServerEnv = System.getProperty(JavaPropertiesSetter.EMS_SERVER_ENV);
		this.emsServerURL = System.getProperty(JavaPropertiesSetter.EMS_SERVER_URL);
		this.emsUserName = System.getProperty(JavaPropertiesSetter.EMS_USER_NAME);
		this.emsUserPassword = System.getProperty(JavaPropertiesSetter.EMS_PASSWORD);
		this.destinationQueue = emsServerEnv + "." + System.getProperty(JavaPropertiesSetter.EMS_QUEUE_NAME);
		this.confirmMessageToServer = System.getProperty(JavaPropertiesSetter.CONFIRM_MESSAGE_TO_SERVER); 
		this.jmsSender = new JmsMessageSender();
		}
	}

    @Override
    public void onCreate(SessionID sessionId) {
        LOGGER.info("Successfully called onCreate for sessionId : " + sessionId);
    }
 
    @Override
    public void onLogon(SessionID sessionId) {
//    	LOGGER.info("Successfully logged on for sessionId :" + sessionId);
//    	if (System.getProperty(JavaPropertiesSetter.EMS_INCOMING_QUEUE_NAME)!=null && System.getProperty(JavaPropertiesSetter.EMS_INCOMING_QUEUE_NAME).length()>0){    		
//    		try {
//    			TibjmsAsyncMsgConsumer queueListner = new TibjmsAsyncMsgConsumer(sessionId);
//    			Session.lookupSession(sessionId).getDataDictionary();
//    		} catch (JMSException e) {
//    			LOGGER.error("Error while initializing queue listener ", e);
//    		}
//    	};
    }
 
    @Override
    public void onLogout(SessionID sessionId) {
    	LOGGER.info("Successfully logged out for sessionId : " + sessionId);
    }

	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectTagValue, RejectLogon {
		LOGGER.info("FROM SERVER (" + this.getMessageType(message) + ") >>> " + message.toString().replaceAll("\1", "|"));
		
	}

	@Override
	public void fromApp(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		String messageContent = message.toString().replaceAll("\1", "|");
		LOGGER.info("FROM APP ("+ this.getMessageType(message) +") >>> " + messageContent);
		String currentDate =  new SimpleDateFormat("ddMMyyyy").format(new Date());
		File folderName = new File(incomingMsgFolderPath + currentDate);
		if (!folderName.exists()){
			folderName.mkdirs();
		}
		String date = new SimpleDateFormat("ddMMYYYY").format(new Date());
		String timeStamp = new SimpleDateFormat("HHmmssSSS").format(new Date());
		String fixId = MessageUtils.getStringField(message.toString(),34);
		String fileName =  folderName.getPath() + FILE_SEPARATOR + fixId + "_" + date + "_"  + timeStamp + FILE_EXTENSION;
		writeTextInFile(fileName,messageContent);
		if (this.pushIncomingMessageToQueue){
			jmsSender.sendMessage(this.emsServerURL,this.emsUserName,this.emsUserPassword,this.destinationQueue,messageContent);
			LOGGER.info("sending message to queue : " + destinationQueue);
		}
		String msgType = message.getHeader().getString(MsgType.FIELD);
		if("true".equals(this.confirmMessageToServer) && msgType.equals(MsgType.TRADE_CAPTURE_REPORT)) {
			try {
				sendTradeCaptureReportAck(message,sessionId);
			} catch (FieldNotFound | SessionNotFound e) {
				e.printStackTrace();
			}	
		}
		
		//Session.sendToTarget(message,sessionId);
	}
	private void sendTradeCaptureReportAck(Message tradeReport, SessionID sessionID) 
		    throws FieldNotFound, SessionNotFound {
		    
		    TradeCaptureReportAck ack = new TradeCaptureReportAck();
		    
		    // Tags obligatoires
		    ack.set(new TradeReportID(tradeReport.getString(TradeReportID.FIELD))); // Tag 571
		    ack.set(new TradeReportTransType(0)); // Tag 487: 0=New
		    ack.set(new TradeReportType(0)); // Tag 856: 0=Submit
		    ack.set(new TrdType(tradeReport.getChar(TrdType.FIELD))); // Tag 828
		    ack.set(new ExecType(ExecType.TRADE)); // Tag 150: F=Trade
		    ack.set(new TradeReportRefID(tradeReport.getString(TradeReportID.FIELD))); // Tag 572
		    
		    // Statut de l'acceptation
		    ack.set(new TrdRptStatus(TrdRptStatus.ACCEPTED)); // Tag 939: 0=Accepted
		    
		    // Copier les parties (si nécessaire selon spec TradeWeb)
		    if (tradeReport.isSetField(TrdMatchID.FIELD)) {
		        ack.set(new TrdMatchID(tradeReport.getString(TrdMatchID.FIELD))); // Tag 880
		    }
		    
		    // Envoyer
		    Session.sendToTarget(ack, sessionID);
		    LOGGER.info("Trade Capture Report ACK sent for TradeReportID: {}", 
		        tradeReport.getString(TradeReportID.FIELD));
		}
	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		LOGGER.info("TO APP (" + getMessageType(message) +") >>> " + message.toString().replaceAll("\1", "|"));
		//LOGGER.info("To App with message : " + message.toString().replaceAll("\1", "|"));
	}

	@Override
	public void toAdmin(quickfix.Message message, SessionID sessionId) {
		LOGGER.info("TO SERVER ("+this.getMessageType(message)+ ") >>> " + message.toString().replaceAll("\1", "|"));
	}


  private void writeTextInFile(String fileName, String content){
  try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
	  	bw.write(content);
		LOGGER.info("Write incoming message in " + fileName);
	} catch (IOException e) {
		LOGGER.error(e.getStackTrace());
	}
  }
  private String getMessageType(Message message){
		try {
			String messageType = MessageUtils.getMessageType(message.toString());
			switch (messageType) {
			case MsgType.HEARTBEAT:
				return "Heart Beat";
			case MsgType.TEST_REQUEST:
				return "Test Request";
			case MsgType.RESEND_REQUEST:
				return "Resend Request";
			case MsgType.REJECT:
				return "Reject";
			case MsgType.SEQUENCE_RESET:
				return "Sequence Reset";
			case MsgType.LOGOUT:
				return "Logout";
			case MsgType.LOGON:
				return "Logon";
			case MsgType.TRADE_CAPTURE_REPORT_REQUEST:
				return "Trade Capture Report Request";
			case MsgType.TRADE_CAPTURE_REPORT_REQUEST_ACK:
				return "Trade Capture Report Request Ack";
			case MsgType.TRADE_CAPTURE_REPORT:
				return "Trade Capture Report";
			default:
				return messageType;
			}
		} catch (InvalidMessage e) {
			return ""; 
		}
	}
}
