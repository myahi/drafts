package fr.lbp.markit.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.swapswire.sw_api.SW_DealNotifyData;

import fr.lbp.markit.configuration.ApplicationProperties;

public class Tools {
	private static final Logger LOGGER = LogManager.getLogger(Tools.class);
	public final static String MARKIT_ACTION_NEW = "NEW";
	public final static String MARKIT_ACTION_DRAFT_NEW = "DRAFT_NEW";
	public final static String MARKIT_ACTION_DRAFT_AMEND = "DRAFT_AMEND";
	public final static String MARKIT_ACTION_DRAFT_CANCEL = "DRAFT_CANCEL";
	public final static String MARKIT_ACTION_RELEASE = "RELEASE";
	public final static String MARKIT_ACTION_AFFIRM = "AFFIRM";
	public final static String MARKIT_ACTION_ACCEPT_AFFIRM = "ACCEPT_AFFIRM";
	public final static String MARKIT_ACTION_PARTIAL_TERMANATE = "PARTIAL_TERMANATE";
	public final static String MARKIT_ACTION_PICKUP = "PICKUP";
	public final static String MARKIT_ACTION_WITHDRAW = "WITHDRAW";
	public final static String MARKIT_ACTION_CANCELLATION = "CANCELLATION";
	public final static String MARKIT_ACTION_EXERCISE = "EXERCISE";
	public final static String MARKIT_ACTION_REJECT = "REJECT";
	public final static String MARKIT_ACTION_TRANSFER = "TRANSFER";
	public final static String MARKIT_ACTION_UPDATE = "UPDATE";
	public final static String MARKIT_ACTION_DRAFT_AND_TRANSFERT = "DRAFT_NEW_AND_TRANSFERT";
	
	public static String prepareSavingFile(String msgBody,String outgingMsgFolderPath){
		String fileName = null;
		String currentDate =  new SimpleDateFormat("ddMMyyyy").format(new Date());
		File folderName = new File(outgingMsgFolderPath + currentDate);
		if (!folderName.exists()){
			folderName.mkdirs();
		}
		return fileName;
	}
	
	public static void writeTextInFile(String fileName, String content){
		  try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
			  	bw.write(content);
				LOGGER.info("Write outgoing message into file >>>" + fileName);
			} catch (IOException e) {
				LOGGER.error(e.getStackTrace());
			}
		  }
	
	public static String extractStackTraceExcetpionInoString(Throwable ex){
		StringWriter stackTrace = new StringWriter();
		ex.printStackTrace(new PrintWriter(stackTrace));
		return stackTrace.toString();
	}
	
	public static String buildErrorMessage(String exception,String fixMessage){
		StringBuffer buffer = new StringBuffer();
		if(exception!=null){
			buffer.append("<exception>").append(exception).append("</exception>");	
		}
		else {
			buffer.append("<exception>").append("</exception>");
		}
		buffer.append("<fixMessage>").append(fixMessage).append("</fixMessage>");
		return buffer.toString();
	}
	
	public static String getMarkitActionType(String messageContent){
		String inputAction = StringUtils.substringBetween(messageContent, "<marketAction>","</marketAction>").trim();
		if("<actionName>NEW</actionName>".equals(inputAction)){
			return MARKIT_ACTION_NEW ;
		}
		else if("<actionName>DRAFT_NEW</actionName>".equals(inputAction)){
			return MARKIT_ACTION_DRAFT_NEW ;
		}
		else if("<actionName>RELEASE</actionName>".equals(inputAction)){
			return MARKIT_ACTION_RELEASE ;
		}
		else if("<actionName>AFFIRM</actionName>".equals(inputAction)){
			return MARKIT_ACTION_AFFIRM ;
		}
		else if("<actionName>PICKUP</actionName>".equals(inputAction)){
			return MARKIT_ACTION_PICKUP ;
		}
		else if("<actionName>WITHDRAW</actionName>".equals(inputAction)){
			return MARKIT_ACTION_WITHDRAW ;
		}
		else if("<actionName>CANCELLATION</actionName>".equals(inputAction)){
			return MARKIT_ACTION_CANCELLATION ;
		}
		else if("<actionName>EXERCISE</actionName>".equals(inputAction)){
			return MARKIT_ACTION_EXERCISE ;
		}
		else if("<actionName>ACCEPT_AFFIRM</actionName>".equals(inputAction)){
			return MARKIT_ACTION_ACCEPT_AFFIRM ;
		}
		else if("<actionName>BILATERAL_AMEND</actionName>".equals(inputAction)){
			return MARKIT_ACTION_PARTIAL_TERMANATE ;
		}
		else if("<actionName>REJECT</actionName>".equals(inputAction)){
			return MARKIT_ACTION_REJECT ;
		}
		else if("<actionName>TRANSFER</actionName>".equals(inputAction)){
			return MARKIT_ACTION_TRANSFER ;
		}
		else if("<actionName>UPDATE</actionName>".equals(inputAction)){
			return MARKIT_ACTION_UPDATE ;
		}
		else if("<actionName>DRAFT_NEW_AND_TRANSFERT</actionName>".equals(inputAction)){
			return MARKIT_ACTION_DRAFT_AND_TRANSFERT ;
		}
		return null;
	}
	public static String getSWDML(String messageContent){
		String inputSWDML = StringUtils.substringBetween(messageContent, "<swdml>","</swdml>");
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + inputSWDML;
	}
	public static String getSWDMLForAffirm(String messageContent){
		String inputSWDML = StringUtils.substringBetween(messageContent, "<swdml>","</swdml>");
		inputSWDML = StringUtils.replace(inputSWDML, "&lt;","<");
		inputSWDML = StringUtils.replace(inputSWDML, "&gt;",">");
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + inputSWDML;
	}
	
	public static String getPrivateData(String messageContent){
		String inputSWDML = StringUtils.substringBetween(messageContent, "<PrivateDataData>","</PrivateDataData>");
		return inputSWDML;
	}
	
	public static String getCancellationPart(String messageContent){
		String inputCancellation = StringUtils.substringBetween(messageContent, "<cancellationPart>","</cancellationPart>");
		return inputCancellation;
	}
	public static String getPartialTerminatePart(String messageContent){
		String inputPartialTerm = StringUtils.substringBetween(messageContent, "<swdml>","</swdml>").trim();
		return inputPartialTerm;
	}
	public static String getExercisePart(String messageContent){
		String inputExercise = StringUtils.substringBetween(messageContent, "<exercisePart>","</exercisePart>");
		return inputExercise;
	}
	
	public static String getRecipienData(String messageContent){
		String recipientData = StringUtils.substringBetween(messageContent, "<RecipientData>","</RecipientData>");
		String recepientOne= null;
		if(recipientData != null && recipientData.contains("<Recipient id=\"two\">")){
			recepientOne = "<Recipient id=\"one\">"+StringUtils.substringBetween(recipientData, "<Recipient id=\"one\">","<Recipient id=\"two\">");	
			if(recepientOne.contains("<UserId/>")){
				return null;
			}
			else {
				return recepientOne;
			}
		}
		else {
			recepientOne = recipientData;
		}
		return recepientOne;
	}
	
	public static String getSecondRecipienData(String messageContent){
		String recipientData = StringUtils.substringBetween(messageContent, "<RecipientData>","</RecipientData>");
		String recepientTwo= null;
		if(recipientData.contains("<Recipient id=\"two\">")){
			recepientTwo = "<Recipient id=\"one\">" + StringUtils.substringBetween(recipientData, "<Recipient id=\"two\">","</Recipient>") + "</Recipient>";	
		}
		else {
			recepientTwo = recipientData;
		}
		return recepientTwo;
	}
	public static String getSinkUpdateData(String messageContent){
		String inputSWDML = StringUtils.substringBetween(messageContent, "<SinkUpdatePart>","</SinkUpdatePart>");
		return inputSWDML;
	}
	public static String getOldDealVersionHandle(String messageContent){
		String oldDealVersionHandle = StringUtils.substringBetween(messageContent, "<dvh>","</dvh>");
		return oldDealVersionHandle;
	}
	
	public static String getMessageText(String messageContent){
		String inputSWDML = StringUtils.substringBetween(messageContent, "<messageTextContent>","</messageTextContent>");
		return inputSWDML;
	}
	public static String getSourceRefMessage(String messageContent){
		String inputMessageReference = StringUtils.substringBetween(messageContent, "<swAdditionalField sequence=\"1\">","</swAdditionalField>");
		return inputMessageReference;
	}

	public static String buildResponseToSource(SW_DealNotifyData dnData,String swml){
		StringBuffer buffer = new StringBuffer();
		buffer.append("<root><swmlDealNotification>");
		buffer.append("<LH>").append(dnData.getLh()).append("</LH>");
		buffer.append("<BrokerDealId>").append(dnData.getBrokerId()).append("</BrokerDealId>");
		buffer.append("<DealID>").append(dnData.getDealId()).append("</DealID>");
		buffer.append("<MajorVersion>").append(dnData.getMajorVer()).append("</MajorVersion>");
		buffer.append("<MinorVersion>").append(dnData.getMinorVer()).append("</MinorVersion>");
		buffer.append("<PrivateVersion>").append(dnData.getPrivateVer()).append("</PrivateVersion>");
		buffer.append("<Side>").append(dnData.getSide()).append("</Side>");
		buffer.append("<PreviousDVH>").append(dnData.getPrevDVH()).append("</PreviousDVH>");
		buffer.append("<DVH>").append(dnData.getDvh()).append("</DVH>");
		buffer.append("<NewState>").append(dnData.getNewState()).append("</NewState>");
		buffer.append("<NewStateString>").append(dnData.getNewStateStr()).append("</NewStateString>");
		buffer.append("<ContractState>").append(dnData.getContractState()).append("</ContractState>");
		buffer.append("<ProductType>").append(dnData.getProductType()).append("</ProductType>");
		buffer.append("<TradeAttrFlags>").append(dnData.getTradeAttrFlags()).append("</TradeAttrFlags>");
		buffer.append("<refMessageId>").append("</refMessageId>");
		buffer.append("</swmlDealNotification>");
		buffer.append("<swml>").append(swml).append("</swml>");
		buffer.append("</root>");
		return buffer.toString();
	}

	public static String buildErrorToSource(String ref,String errorCode, String stackTrace){
		StringBuffer buffer = new StringBuffer();
		buffer.append("<root>");
		buffer.append("<messageId>").append(ref).append("</messageId>");
		buffer.append("<errorCode>").append(errorCode).append("</errorCode>");
		buffer.append("<stackTrace>").append(stackTrace.replace("<","&lt;")).append("</stackTrace>");
		buffer.append("</root>");
		return buffer.toString();
	}

	public static String buildReponseToSourceForDraft(String ref,String dvh){
		StringBuffer buffer = new StringBuffer();
		buffer.append("<root><swmlDealNotification>");
		buffer.append("<LH>").append("</LH>");
		buffer.append("<BrokerDealId>").append("</BrokerDealId>");
		buffer.append("<DealID>").append("</DealID>");
		buffer.append("<MajorVersion>").append("</MajorVersion>");
		buffer.append("<MinorVersion>").append("</MinorVersion>");
		buffer.append("<PrivateVersion>").append("</PrivateVersion>");
		buffer.append("<Side>").append("</Side>");
		buffer.append("<PreviousDVH>").append("</PreviousDVH>");
		buffer.append("<DVH>").append(dvh).append("</DVH>");
		buffer.append("<NewState>").append("</NewState>");
		buffer.append("<NewStateString>").append("</NewStateString>");
		buffer.append("<ContractState>").append("</ContractState>");
		buffer.append("<ProductType>").append("</ProductType>");
		buffer.append("<TradeAttrFlags>").append("</TradeAttrFlags>");
		buffer.append("<refMessageId>").append(ref).append("</refMessageId>");
		buffer.append("</swmlDealNotification>");
		buffer.append("<swml>").append("</swml>");
		buffer.append("</root>");
		return buffer.toString();
		
}

	public static String getCurrentTimeStamp(){
		Instant instant = Instant.now();
	    // Set the time zone to GMT
	    ZoneId zone = ZoneId.of("GMT");
	    // Format the date and time as a string
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    String dateTime = instant.atZone(zone).format(formatter);
	    return dateTime;
		
	}
	
	public static void updateLastExchangeTimeStamp(String exchangeTimeStamp){
		  try (BufferedWriter bw = new BufferedWriter(new FileWriter( ApplicationProperties.LBP_MARKIT_LAST_EXCHANGE_DATE))) {
			  bw.write(exchangeTimeStamp);
			  bw.flush();
			  bw.close();
				LOGGER.info("Last reception date updated >> " + exchangeTimeStamp);
			} catch (IOException e) {
				LOGGER.error(e.getStackTrace());
			}
	  }

}
