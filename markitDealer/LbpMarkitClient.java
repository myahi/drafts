package fr.lbp.markit.controller;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.swapswire.sw_api.SWAPILinkModule;

import fr.lbp.jms.connection.MessageHandler;
import fr.lbp.markit.configuration.ApplicationProperties;
import fr.lbp.markit.connection.DealerAPIWrapper;
import fr.lbp.markit.connection.ErrorCode;
import fr.lbp.markit.connection.Session;

public class LbpMarkitClient {
	private static Session MARKIT_SESSION;
	private static MessageHandler MESSAGE_HANDLER = null;
	public static int current_connection_error_account = 0;
	public static volatile boolean IS_MARKIT_SESSION_CONNECTED = false;
	public static volatile boolean DISCONNECTION_REQUEST_RECEIVED = false;
	private static final SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	Integer maxConnectionRetryCount = Integer.valueOf(ApplicationProperties.LBP_MARKIT_MAX_RETRY_COUNT);
	private static ExecutorService executorService;
	
	static {
		System.load(System.getenv("LD_LIBRARY_PATH_SO"));
	}
	
	private static final Logger LOGGER = LogManager.getLogger(LbpMarkitClient.class);

	public LbpMarkitClient(){
		try {
			this.initializeMarkitConnector(false);
			DISCONNECTION_REQUEST_RECEIVED = false;
		} catch (JMSException | ErrorCode e) {
			LOGGER.error(e.getMessage(),e);
		}
	}
	public boolean isConnected(){
		return IS_MARKIT_SESSION_CONNECTED;
	}
	public boolean isMarkitSessionConnected(){
		return MARKIT_SESSION != null && MARKIT_SESSION.isValidClientSession();
	}
	public void initializeMarkitConnector(boolean resetRetryCount) throws JMSException, ErrorCode {
		LOGGER.info("initializeMarkitConnector: "  + IS_MARKIT_SESSION_CONNECTED + " " + DISCONNECTION_REQUEST_RECEIVED);
		if(resetRetryCount){
			current_connection_error_account = 0 ;
			DISCONNECTION_REQUEST_RECEIVED = false ;
		}
		if(!IS_MARKIT_SESSION_CONNECTED  && !DISCONNECTION_REQUEST_RECEIVED){
		this.executorService = Executors.newFixedThreadPool(1);
		MESSAGE_HANDLER = new MessageHandler();
		MESSAGE_HANDLER.setLbpMarkitClient(this);
		String[] libraryVersion = { new String() };
		LOGGER.info("SW_API_DLL Version = " + libraryVersion[0]);
		//LOGGER.info("Max connection retry count: " + maxConnectionRetryCount);
        String markitHost = ApplicationProperties.LBP_MARKTI_HOST_NAME;
        String markitPort = ApplicationProperties.LBP_MARKIT_PORT;
        String markitUserName = ApplicationProperties.LBP_MARKIT_USER_NAME;
        String markitPassword = ApplicationProperties.LBP_MARKIT_PASSWORD;
        Session session = new Session(markitHost + ":" + markitPort, markitUserName , markitPassword, Integer.valueOf(ApplicationProperties.LBP_MARKIT_CONNECTION_TIMEOUT));
        executorService.execute(new Runnable() {
            public void run() {
            	ErrorCode ret = new ErrorCode(SWAPILinkModule.SWERR_Success);
            	do {
                    try {
                        DealerAPIWrapper.poll(Integer.valueOf(ApplicationProperties.LBP_MARKIT_CONNECTION_TESTDURATION), Integer.valueOf(ApplicationProperties.LBP_MARKIT_CONNECTION_TIMEOUT));
                        if(MARKIT_SESSION !=null && IS_MARKIT_SESSION_CONNECTED && !DISCONNECTION_REQUEST_RECEIVED){
                    		String[] books = {new String()};
                    		DealerAPIWrapper.getBookList(MARKIT_SESSION.getLoginHandle(), books);
                    		IS_MARKIT_SESSION_CONNECTED = true;
                    		current_connection_error_account = 0;
                    	}
                        if (!IS_MARKIT_SESSION_CONNECTED && !DISCONNECTION_REQUEST_RECEIVED) {
                            session.deregisterCallbacks();
                            
                            session.disconnect();
                            session.connect();
                            MESSAGE_HANDLER.startListening();
                            session.setMessageHandler(MESSAGE_HANDLER);
                            MARKIT_SESSION = session;
                            current_connection_error_account = 0;
                            IS_MARKIT_SESSION_CONNECTED = true;
                        }
                    } catch (ErrorCode err) {
                    	if(current_connection_error_account > maxConnectionRetryCount){
                    		break;
                    	}
                    	else {
                    		handleConnectionIssue(err);
                    	}
                    	
                    } 
                    catch (JMSException e) {
						LOGGER.error(e);
					}
                    if (Thread.interrupted()){
            			break;
            		}
                } while ((current_connection_error_account < maxConnectionRetryCount) && (ret.isSuccess() || ret.isTimeout()));
            }
        });
		}
		else if(IS_MARKIT_SESSION_CONNECTED) {
			LOGGER.warn("Markit dealer connector already started");
		}
	}
	private void handleConnectionIssue(ErrorCode err){
		LOGGER.error("Exception accured while try connection retry account " + current_connection_error_account);
		LOGGER.error(err);
		IS_MARKIT_SESSION_CONNECTED = false;
		current_connection_error_account ++;
		if (current_connection_error_account >= maxConnectionRetryCount){
			LOGGER.error("Maximum try number has been reached");
			try {
				//if(IS_MARKIT_SESSION_CONNECTED){
					stopMarkitConnector();
					Date currentDate = new Date();
			        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
					MESSAGE_HANDLER.senTechnicalErrorMessage(dateFormat.format(currentDate)+": Le nombre de tentative de connexion a été atteint sur le connecteur markitdealer\n le connecteur a été arrété.");					
				//}
			} catch (JMSException e1) {
				LOGGER.error(e1);
			}
		}
		else {
			LOGGER.info("Current connection retry count: " + current_connection_error_account);
			LOGGER.info("Max connection retry count: " + maxConnectionRetryCount);
			try {
				if(MESSAGE_HANDLER!=null){
					MESSAGE_HANDLER.stopListening();
				}
				initializeMarkitConnector(false);
			} catch (JMSException | ErrorCode error) {
				LOGGER.error(error);
			}
		}
	}
	public static void loggedOut() throws ErrorCode {
		DealerAPIWrapper.logout(MARKIT_SESSION.getLoginHandle());
		DISCONNECTION_REQUEST_RECEIVED = true;
		IS_MARKIT_SESSION_CONNECTED = false;
	}
	public static void stopMarkitConnector() throws JMSException{
		try {
			DISCONNECTION_REQUEST_RECEIVED = true;
			IS_MARKIT_SESSION_CONNECTED = false;
			if(executorService!=null){
				executorService.awaitTermination(5, TimeUnit.SECONDS);
				executorService.shutdownNow();				
			}
			executorService = null;
		} catch (InterruptedException error) {
			LOGGER.error(error);
		}
		if(MESSAGE_HANDLER!=null){
			MESSAGE_HANDLER.stopListening();
		}
		if(MARKIT_SESSION != null){
				try {
					DealerAPIWrapper.logout(MARKIT_SESSION.getLoginHandle());
					//DealerAPIWrapper.disconnect(MARKIT_SESSION.getLoginHandle());
					IS_MARKIT_SESSION_CONNECTED = false;
					LOGGER.info("Session logout");
					//isConnected = false;
				} catch (ErrorCode exception) {
					//LOGGER.error(exception.getMessage(),exception);
				}
	}
		LOGGER.info("Markit connector stopped");
	}
//	private static void recoveryDeals(){
//		String[] recoverdDeals = {new String()};
//		String lastExhangeTimeStamp = Tools.getLastUTCTimeStampReception();
//		if(lastExhangeTimeStamp != null && lastExhangeTimeStamp.length()>0 ){
//			String queryDealXml = Tools.buildDealQuery(lastExhangeTimeStamp, Tools.getCurrentTimeStamp());
//			LOGGER.info(" start queryDealXml >> "  + queryDealXml);
//			try {
//				//recoverdDeals = DealerAPIWrapper.queryDeals(queryDealXml);
//				DealerAPIWrapper.queryDeals(MARKIT_SESSION.getLoginHandle(), queryDealXml, recoverdDeals);
//				System.out.println(recoverdDeals[0]);
//			} catch (ErrorCode e) {
//				LOGGER.error(e.getMessage(),e);
//			}
//		}
//	}
//	
//	private static void getActiveDeals(){
//		LOGGER.info("start get active deals");
//		String[] activeDeals = {new String()};
//		List<String> activeDealsTradeId = new ArrayList<String>();
//    	if(MARKIT_SESSION.isValidClientSession()){
//    		try {
//				DealerAPIWrapper.getActiveDealInfo(MARKIT_SESSION.getLoginHandle(), activeDeals);
//				Pattern tradeIdPattern = Pattern.compile("\\<tradeId>(.*?)\\</tradeId>");
//				Matcher matcher = tradeIdPattern.matcher(activeDeals[0]);
//				while (matcher.find()) {
//			        String tradeId = matcher.group(1);
//			        activeDealsTradeId.add(tradeId);
//			        }
//				for (String tradeId : activeDealsTradeId) {
//					
//				}
//			} catch (ErrorCode e) {
//				LOGGER.error(e.getMessage(),e);
//			}
//    	}
//	}
//	private static void getAllDealVersionHandles(String tradeId){
//		String lastExhangeTimeStamp = Tools.getLastUTCTimeStampReception();
//		if(lastExhangeTimeStamp != null && lastExhangeTimeStamp.length()>0 ){
//			String queryDealXml = Tools.buildDealQuery(lastExhangeTimeStamp, Tools.getCurrentTimeStamp());
//			LOGGER.info("queryDealXml >> "  + queryDealXml);
//			try {
//				String[] dealVersionHandles;
//				DealerAPIWrapper.getAllDealVersionHandles(MARKIT_SESSION.getLoginHandle(), tradeId, contractVersion, side, dealVersionHandles);
//			} catch (IOException |ErrorCode e) {
//				LOGGER.error(e.getMessage(),e);
//			}
//		}
//	}
	
	
	public static String[] getAllDealVersionHandles(String tradeId,String contractVersion, String side) throws NumberFormatException, ErrorCode{
			String[] dealVersionHandles = {new String()};
			DealerAPIWrapper.getAllDealVersionHandles(MARKIT_SESSION.getLoginHandle(), Long.valueOf(tradeId), Integer.valueOf(contractVersion), Integer.valueOf(side), dealVersionHandles);
			return dealVersionHandles;
	}

	public static String getSWDML(String dealVersionHandle,String privateDataXML, String recipientXML, String messageText) throws IOException, ErrorCode{
    	String[] newDealVersionHandle = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
    		DealerAPIWrapper.getDealSWDML(MARKIT_SESSION.getLoginHandle(), "SWDML_4_2: 4.2", dealVersionHandle, newDealVersionHandle);
    		LOGGER.info("Deal Submitted To Markit " + newDealVersionHandle[0]);
    		return newDealVersionHandle[0];
    	}
    	return null;
	}
	
	public static String submitNewDeal(String swdml,String privateDataXML, String recipientXML, String messageText) throws IOException, ErrorCode{
    	String[] newDealVersionHandle = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
    		DealerAPIWrapper.submitNewDeal(MARKIT_SESSION.getLoginHandle(), swdml, privateDataXML, recipientXML, messageText, newDealVersionHandle);
    		LOGGER.info("Deal Submitted To Markit " + newDealVersionHandle[0]);
    		return newDealVersionHandle[0];    		
    	}
    	return null;
	}
	
	public static String submitDraftNewDeal(String swdml,String privateDataXML, String recipientXML, String messageText) throws IOException, ErrorCode{
    	String[] newDealVersionHandle = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
    		DealerAPIWrapper.submitDraftNewDeal(MARKIT_SESSION.getLoginHandle(), swdml, privateDataXML, recipientXML, messageText, newDealVersionHandle);
    		LOGGER.info("Draft deal submitted To Markit " + newDealVersionHandle[0]);
    		return newDealVersionHandle[0];    		
    	}
    	return null;
	}
	public static String releaseDeal(String privateDataXML, String oldDealVersionHandle) throws IOException, ErrorCode{
    	String[] newDealVersionHandle = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
    		DealerAPIWrapper.release(MARKIT_SESSION.getLoginHandle(), oldDealVersionHandle, privateDataXML, newDealVersionHandle);
    		return newDealVersionHandle[0];    		
    	}
    	return null;
	}
	public static String cancelDeal(String privateDataXML, String oldDealVersionHandle) throws IOException, ErrorCode{
    	String[] newDealVersionHandle = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
    		DealerAPIWrapper.release(MARKIT_SESSION.getLoginHandle(), oldDealVersionHandle, privateDataXML, newDealVersionHandle);
    		return newDealVersionHandle[0];    		
    	}
    	return null;
	}
	
	public static String rejectDeal(String oldDealVersionHandle, String messageText) throws IOException, ErrorCode{
    	String[] newDealVersionHandle = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
    		DealerAPIWrapper.rejectDK(MARKIT_SESSION.getLoginHandle(), oldDealVersionHandle, messageText, newDealVersionHandle);
    		return newDealVersionHandle[0];    		
    	}
    	return null;
	}
	
	public static String transferDeal(String oldDealVersionHandle,String privateDataXML,String recipientXML, String messageText) throws IOException, ErrorCode{
    	String[] newDealVersionHandle = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
    		DealerAPIWrapper.transfer(MARKIT_SESSION.getLoginHandle(), oldDealVersionHandle, privateDataXML, recipientXML, messageText, newDealVersionHandle);
    		return newDealVersionHandle[0];    		
    	}
    	return null;
	}
	
	public static String updateDeal(String oldDealVersionHandle,String sinkUpdateXML) throws IOException, ErrorCode{
    	String[] newDealVersionHandle = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
    		DealerAPIWrapper.update(MARKIT_SESSION.getLoginHandle(), oldDealVersionHandle, sinkUpdateXML, newDealVersionHandle);
    		return newDealVersionHandle[0];    		
    	}
    	return null;
	}
	
	public static String submitPostTradeEvent(String privateDataXML, String oldDealVersionHandle, String postTradeXML,String recipientXML, String messageText) throws IOException, ErrorCode{
    	String[] newDealVersionHandle = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
    		DealerAPIWrapper.submitPostTradeEvent(MARKIT_SESSION.getLoginHandle(), oldDealVersionHandle, postTradeXML, privateDataXML, recipientXML, messageText, newDealVersionHandle);
    		return newDealVersionHandle[0];    		
    	}
    	return null;
	}
	
	public static String pickUpDeal(String oldDealVersionHandle,String privateDataXML) throws IOException, ErrorCode{
		String[] newDealVersionHandleForPickUp = {new String()};
		if(MARKIT_SESSION.isValidClientSession()){
			DealerAPIWrapper.pickup(MARKIT_SESSION.getLoginHandle(), oldDealVersionHandle, privateDataXML, newDealVersionHandleForPickUp);
			return newDealVersionHandleForPickUp[0];
		}
		return null;
	}
	
	public static String affirmDeal(String oldDealVersionHandle,String privateDataXML,String swdml) throws IOException, ErrorCode{
    	
    	String[] newDealVersionHandleForAffirm = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
				LOGGER.info("Sending affirm for DVH:  " + oldDealVersionHandle);
				LOGGER.info("Sending affirm with PrivateDataXML:  " + privateDataXML);
				LOGGER.info("Sending affirm with swdml:  " + swdml);
				DealerAPIWrapper.affirm(MARKIT_SESSION.getLoginHandle(), oldDealVersionHandle,swdml, privateDataXML,"AFFIRM FROM API CLIENT", newDealVersionHandleForAffirm);
				return newDealVersionHandleForAffirm[0];
			}
    	return null;
	}
	
public static String acceptAffirmDeal(String oldDealVersionHandle,String privateDataXML) throws IOException, ErrorCode{
    	String[] newDealVersionHandleForAcceptAffirm = {new String()};
    	if(MARKIT_SESSION.isValidClientSession()){
				LOGGER.info("Sending acceptAffirm with DVH:  " + oldDealVersionHandle);
				LOGGER.info("Sending acceptAffirm with PrivateDataXML:  " + privateDataXML);
				DealerAPIWrapper.acceptAffirm(MARKIT_SESSION.getLoginHandle(), oldDealVersionHandle, privateDataXML, newDealVersionHandleForAcceptAffirm);
				return newDealVersionHandleForAcceptAffirm[0];
			}
    	return null;
	}
	
	public static String transfer(String oldDealVersionHandle,String privateDataXML, String transferRecipientXML, String messageText) throws IOException, ErrorCode{
    	String[] newDealVersionHandle = {new String()};
    		DealerAPIWrapper.transfer(MARKIT_SESSION.getLoginHandle(), oldDealVersionHandle, privateDataXML, transferRecipientXML, messageText, newDealVersionHandle);
    		LOGGER.info("Draft deal submitted To Markit " + newDealVersionHandle[0]);
    		return newDealVersionHandle[0];
	}
	
	public static String[] getMyInterestGroups() throws IOException, ErrorCode{
    		String[] resultXML = {new String()};
    		DealerAPIWrapper.getMyInterestGroups(MARKIT_SESSION.getLoginHandle(), resultXML);
    		LOGGER.info("Get my InterestGroups " + String.join(";", resultXML));
    		return resultXML;
	}
	
	public static String[] getMyUserInfo() throws IOException, ErrorCode{
		String[] resultXML = {new String()};
		DealerAPIWrapper.getMyuserInfo(MARKIT_SESSION.getLoginHandle(), resultXML);
		LOGGER.info("Get my user infrmations " + String.join(";", resultXML));
		return resultXML;
	}
	public static int getDealGetMySide(long dealId) throws IOException, ErrorCode{
		int[] side_out = {new Integer(1)};
		DealerAPIWrapper.getDealGetMySide(MARKIT_SESSION.getLoginHandle(), dealId, side_out);
		LOGGER.info("getDealGetMySide for trade Id : " + dealId + " " + side_out[0]);
		return side_out[0];
	}
	
	
	public static String[] queryDeals(String queryXML) throws IOException, ErrorCode{
    	String[] resultsXML = {new String()};
    		DealerAPIWrapper.queryDeals(MARKIT_SESSION.getLoginHandle(), queryXML, resultsXML);
    		for (String result : resultsXML) {				
    			LOGGER.info("Qurrey deals result: " + result);
			}
    		return resultsXML;
	}
	
	public static String getDealSWML(String dealVersionHandle) throws ErrorCode{
		String[] swdml = {new String()};
		LOGGER.info("DealGetSWDML for dvh: " + dealVersionHandle);
		DealerAPIWrapper.getDealSWML(MARKIT_SESSION.getLoginHandle(), "4.2",dealVersionHandle, swdml);
		return swdml[0];
	}
	public static String getDealSWDML(String dealVersionHandle) throws ErrorCode{
		String[] swdml = {new String()};
		LOGGER.info("DealGetSWDML for dvh: " + dealVersionHandle);
		DealerAPIWrapper.getDealSWDML(MARKIT_SESSION.getLoginHandle(), "4.2", dealVersionHandle, swdml);
		return swdml[0];
	}
	public static String withdrawDeal(String dealVersionHandle,String privateDataXML) throws ErrorCode{
		LOGGER.info("Withdraw for dvh: " + dealVersionHandle);
		String[] newDealVersionHandleForPull = {new String()};
		DealerAPIWrapper.pull(MARKIT_SESSION.getLoginHandle(), dealVersionHandle, newDealVersionHandleForPull);
		String[] newDealVersionHandleForWithdrow = {new String()};
		DealerAPIWrapper.withdraw(MARKIT_SESSION.getLoginHandle(), newDealVersionHandleForPull[0], newDealVersionHandleForWithdrow);
		return newDealVersionHandleForWithdrow[0];
	}


}
