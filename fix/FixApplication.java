package fr.labanquepostale.fix.connection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.labanquepostale.fix.connection.config.ApplicationProperties;
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
import quickfix.field.CollAsgnID;
import quickfix.field.CollAsgnReason;
import quickfix.field.CollAsgnRespType;
import quickfix.field.CollRespID;
import quickfix.field.MsgType;
import quickfix.field.TradeReportID;
import quickfix.field.TradeReportTransType;
import quickfix.field.TransactTime;
import quickfix.fix50sp2.CollateralResponse;
import quickfix.fix50sp2.TradeCaptureReportAck;


public class FixApplication implements Application {
	
	private final ApplicationProperties props;
	private static final Logger LOGGER = LoggerFactory.getLogger(FixClientStarter.class);
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
	private boolean isLbpConfirmMessageToServer;
	
	public FixApplication(ApplicationProperties props){
		this.props = props;
		this.incomingMsgFolderPath = props.getLbpIncomingNotificationFolderPath();
		this.pushIncomingMessageToQueue = this.props.isLbpEmsSendIncomingMessage();
		if (pushIncomingMessageToQueue){
		this.emsServerEnv = this.props.getLbpEmsServerEnv();
		this.emsServerURL = this.props.getLbpEmsServerUrl();
		this.emsUserName = this.props.getLbpEmsUserName();
		this.emsUserPassword = this.props.getLbpEmsPassword();
		this.destinationQueue = emsServerEnv + "." + this.props.getLbpEmsQueueName();
		this.isLbpConfirmMessageToServer = this.props.isLbpConfirmMessageToServer(); 
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
	public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectTagValue, RejectLogon {
		LOGGER.info("FROM SERVER (" + this.getMessageType(message) + ") >>> " + message.toString().replaceAll("\1", "|"));

	}

	@Override
	public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
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
		if(this.isLbpConfirmMessageToServer && msgType.equals(MsgType.TRADE_CAPTURE_REPORT)) {
			try {
				sendTradeCaptureReportAck(message,sessionId);
			} catch (FieldNotFound | SessionNotFound e) {
				e.printStackTrace();
			}	
		}
		else if(this.isLbpConfirmMessageToServer && msgType.equals(MsgType.COLLATERAL_ASSIGNMENT)) {
			try {
				sendCollateralAssignmentAck(message,sessionId);
			} catch (FieldNotFound | SessionNotFound e) {
				e.printStackTrace();
			}	
		}
	}
	
	private void sendTradeCaptureReportAck(Message message, SessionID sessionID) throws FieldNotFound, SessionNotFound {
		TradeCaptureReportAck ack = new TradeCaptureReportAck();
		ack.set(new TradeReportID(message.getString(TradeReportID.FIELD)));
		ack.set(new TradeReportTransType(0));
		Session.sendToTarget(ack, sessionID);
		LOGGER.info("Trade Capture Report ACK sent for TradeReportID: {}", message.getString(TradeReportID.FIELD));
	}
	
	private void sendCollateralAssignmentAck(Message ay, SessionID sessionID) throws FieldNotFound, SessionNotFound {
		CollateralResponse az = new CollateralResponse();
		az.set(new CollRespID("AZ-" + System.currentTimeMillis()));
		if (ay.isSetField(CollAsgnID.FIELD)) {
			az.set(new CollAsgnID(ay.getString(CollAsgnID.FIELD)));
		}
		if (ay.isSetField(CollAsgnReason.FIELD)) {
			az.set(new CollAsgnReason(ay.getInt(CollAsgnReason.FIELD)));
		}
		az.set(new CollAsgnRespType(CollAsgnRespType.RECEIVED));
		az.set(new TransactTime(LocalDateTime.now()));
		Session.sendToTarget(az, sessionID);
	}
	
	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		LOGGER.info("TO APP (" + getMessageType(message) +") >>> " + message.toString().replaceAll("\1", "|"));
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
		LOGGER.error(e.getMessage());
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
