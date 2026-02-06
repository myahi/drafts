package fr.lbp.markit.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.lbp.markit.alerting.MarkitMailService;
import fr.lbp.markit.configuration.ApplicationProperties;
import fr.lbp.markit.connection.ErrorCode;
import fr.lbp.markit.tools.ResponseMessage;
import fr.lbp.markit.tools.Tools;

@Controller
public class MarkitController {
	private static final Logger LOGGER = LogManager.getLogger(MarkitController.class);
	
	private final LbpMarkitClient markitApplication;
	
	public MarkitController(MarkitMailService mailService,ConfigurableApplicationContext applicationContext){
		this.markitApplication = new LbpMarkitClient(mailService);
		LbpMarkitClient.setApplicationContext(applicationContext);
	}
	@RequestMapping(value = "/restartmarkitconnector")
	@ResponseBody
	public ResponseEntity<ResponseMessage> restartFixConnector() {
		LOGGER.info("Receive restart request");
		
			try {
				markitApplication.restartConnector();
				return ResponseEntity.status(HttpStatus.OK)
						.body(new ResponseMessage("Markitdealer connector started !"));
			} catch (RuntimeException | ErrorCode e) {
				LOGGER.error(e.getMessage(), e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
						new ResponseMessage("Could not start connector, for more details please check the log files"));
			}
		}
	
	@RequestMapping(value = "/stopmarkitconnector")
	@ResponseBody
	public ResponseEntity<ResponseMessage> stopFixConnector() {
		LOGGER.info("Receive stop request");
		if (this.markitApplication.isConnected()) {
			try {
				markitApplication.stopMarkitConnector();
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
						new ResponseMessage("Could not stop connector, for more details please check the log files"));
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("Markitdealer connector stopped !"));
	}
	
	@RequestMapping(value = "/statusmarkitconnector")
	@ResponseBody
	public ResponseEntity<ResponseMessage> sessionStatus() {
		if (!this.markitApplication.isConnected()) {
			return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("Markit session not connected"));
		} else {
			return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("Markitdealer session connected"));
		}
	}
	 
	 @GetMapping("getswdml/{dvh}")
	 @ResponseBody
	 public ResponseEntity<ResponseMessage> getSWDML(@PathVariable String dvh) {
		 LOGGER.info("Receive get SWDML for DVH: {0}" , dvh);
		 if(this.markitApplication.isConnected()) {
			 try {
				String swdml = LbpMarkitClient.getDealSWDML(dvh);
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(swdml));
			} catch (ErrorCode e) {
				LOGGER.error(e);
				if (e instanceof ErrorCode){
					ErrorCode errorCode = (ErrorCode) e;
					return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(errorCode.errorString));
				}
				else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					String sStackTrace = sw.toString();
					return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sStackTrace));					
				}
			}
		 }
		 else{
			 return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("Markitdealer session not connected"));
		 }
	 }
	 
	 @GetMapping("getswml/{dvh}")
	 @ResponseBody
	 public ResponseEntity<ResponseMessage> getSWML(@PathVariable String dvh) {
		 LOGGER.info("Receive get SWML for DVH: {0}",dvh);
		 if(this.markitApplication.isConnected()) {
			 try {
				String swml = LbpMarkitClient.getDealSWML(dvh);
				LOGGER.info("Found SWML {0}", swml);
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(swml));
			} catch (ErrorCode e) {
				LOGGER.error(e);
				if (e instanceof ErrorCode){
					ErrorCode errorCode = (ErrorCode) e;
					return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(errorCode.errorString));
				}
				else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					String sStackTrace = sw.toString();
					return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sStackTrace));					
				}
			}
		 }
		 else{
			 return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("Markitdealer session not connected"));
		 }
	 }
	 @RequestMapping(value = "/getLastDVHForTradeId", method = RequestMethod.GET)
		@ResponseBody
		public ResponseEntity<ResponseMessage> getLastDealsVersionHandles(@RequestParam(value="tradeId") String tradeId) {
		 LOGGER.info("getLastDealsVersionHandles with tradeIds: "+ tradeId);
		 String response = "";
			try {
					int dealSide = LbpMarkitClient.getDealGetMySide(Long.valueOf(tradeId));
					LOGGER.info("getLastDealsVersionHandles deal side found: "+ dealSide + " for trade id: "+ tradeId);
					String[] dvhs = LbpMarkitClient.getAllDealVersionHandles(tradeId, "-1", String.valueOf(dealSide));
					LOGGER.info("getLastDealsVersionHandles dvh found: "+ dealSide + " for trade id: "+ String.join(";", dvhs));
					if(dvhs != null && dvhs.length >0){
						String dvhArray[] = dvhs[0].split(System.lineSeparator());
						LOGGER.info("getLastDealsVersionHandles last dvh found: "+ dvhArray[0] + " for trade: " + tradeId);
						response += dvhArray[0] ;
					}
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(response));
			} catch (ErrorCode errorCode) {
				LOGGER.error(errorCode);
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(errorCode.errorString));
			} catch (IOException | NumberFormatException e) {
				LOGGER.error(e);
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				String sStackTrace = sw.toString();
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sStackTrace));
			} 
		}
	 
	 
	 @RequestMapping(value = "/getAllDVHForTradeIdList", method = RequestMethod.GET)
		@ResponseBody
		public ResponseEntity<ResponseMessage> getAllDealsVersionHandles(@RequestParam(value="tradeIds") String tradeIds,@RequestParam(value="onlyLastDVH") Boolean onlyLastDVH) {
		 LOGGER.info("getAllDVHForTradeIdList with tradeIds: "+ tradeIds + " onlyLastDVH "+ onlyLastDVH);
		 String response = "tradeId;dvh\n";
			try {
				for (String currentTradeId : tradeIds.split(";")) {
					int dealSide = LbpMarkitClient.getDealGetMySide(Long.valueOf(currentTradeId));
					LOGGER.info("getAllDVHForTradeIdList deal side found: "+ dealSide + " for trade id: "+ currentTradeId);
					String[] dvhs = LbpMarkitClient.getAllDealVersionHandles(currentTradeId, "-1", String.valueOf(dealSide));
					LOGGER.info("getAllDVHForTradeIdList dvh found: "+ dealSide + " for trade id: "+ String.join(";", dvhs));
					if(dvhs != null && dvhs.length >0){
						String dvhArray[] = dvhs[0].split(System.lineSeparator());
						for (String currentDVH : dvhArray) {
							if(onlyLastDVH != null && onlyLastDVH == true){
								String currentResponse = currentTradeId + ";" + currentDVH + "\n";
								response += currentResponse ;
								break;
							}
							else {
								String currentResponse = currentTradeId + ";" + currentDVH + "\n";
								response += currentResponse ;
							}
						}
					}
				}
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(response));
			} catch (ErrorCode errorCode) {
				LOGGER.error(errorCode);
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(errorCode.errorString));
			} catch (IOException | NumberFormatException e) {
				LOGGER.error(e);
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				String sStackTrace = sw.toString();
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sStackTrace));
			} 
		}

		@RequestMapping(value = "/pickupMarkitDeal", method = RequestMethod.GET)
		@ResponseBody
		public ResponseEntity<ResponseMessage> pickUpMarkitDeal(@RequestParam(value = "tradeIds") String tradeIds) {
			LOGGER.info("pickUpMarkitDeal with tradeIds: {0}", tradeIds);
			String response = "tradeId;lastDVH\n";
			String privatDate = "<PrivateData><swTradingBookId>LBPPAR</swTradingBookId></PrivateData>";
			try {
				for (String currentTradeId : tradeIds.split(";")) {
					int dealSide = LbpMarkitClient.getDealGetMySide(Long.valueOf(currentTradeId));
					String[] dvhs = LbpMarkitClient.getAllDealVersionHandles(currentTradeId, "-1",
							String.valueOf(dealSide));
					if (dvhs != null && dvhs.length > 0) {
						String dvhArray[] = dvhs[0].split(System.lineSeparator());
						for (String currentDVH : dvhArray) {
							String lastDVH = LbpMarkitClient.pickUpDeal(currentDVH, privatDate);
							String currentResponse = currentTradeId + ";" + lastDVH + "\n";
							response += currentResponse + "\n";
						}
					}
				}
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(response));
			} catch (ErrorCode errorCode) {
				LOGGER.error(errorCode);
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
						.body(new ResponseMessage(errorCode.errorString));
			} catch (IOException | NumberFormatException e) {
				LOGGER.error(e);
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				String sStackTrace = sw.toString();
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sStackTrace));
			}
		}

		@RequestMapping(value = "/getMyInterestGroups", method = RequestMethod.GET)
		@ResponseBody
		public ResponseEntity<ResponseMessage> getMyInterestGroups() {
			LOGGER.info("getMyInterestGroups with no parameters");
			try {
				String[] interestGroups = LbpMarkitClient.getMyInterestGroups();
				String interestGroupsList = String.join(";", interestGroups);
				Pattern pattern = Pattern.compile("(<GroupId>)(.*?)(</GroupId>)");
				Matcher matcher = pattern.matcher(interestGroupsList);
				List<String> groupIds = new ArrayList<String>();
				while (matcher.find()) {
					if (null != matcher.group(2) && matcher.group(2).length() > 0) {
						groupIds.add(matcher.group(2));
					}
				}
				String response = String.join(";", groupIds);
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(response));
			} catch (ErrorCode errorCode) {
				LOGGER.error(errorCode);
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
						.body(new ResponseMessage(errorCode.errorString));
			} catch (NumberFormatException | IOException e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				String sStackTrace = sw.toString();
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sStackTrace));
			}
		}
 
				
		@RequestMapping(value = "/transferTo", method = RequestMethod.GET)
		@ResponseBody
		public ResponseEntity<ResponseMessage> transferTo(@RequestParam(value = "markitTradeId") String markitTradeId,@RequestParam(value = "groupId") String markitGroupId) {
			LOGGER.info("transferTo with markitTradeId: {0} to groups: {1} and comment: {2}", markitTradeId,
					markitGroupId);
			try {
				String transferRecipientXMLTemplate = "<Recipient id=\"one\"><ParticipantId>#ParticipantId#</ParticipantId><GroupId>#GroupId#</GroupId></Recipient>";
				String[] userInformations;
				userInformations = LbpMarkitClient.getMyUserInfo();
				String userInformationAsString = String.join(";", userInformations);
				String participantId = StringUtils.substringBetween(userInformationAsString, "id=\"", "\"");
				int dealSide = LbpMarkitClient.getDealGetMySide(Long.valueOf(markitTradeId));
				String[] dvh = LbpMarkitClient.getAllDealVersionHandles(markitTradeId, "-1", String.valueOf(dealSide));
				if (dvh.length > 0) {
					String lastDVH = dvh[0].split(System.lineSeparator())[0];
					String transferRecipientXML = transferRecipientXMLTemplate.replace("#ParticipantId#",
							participantId);
					transferRecipientXML = transferRecipientXML.replace("#GroupId#", markitGroupId);
					String newDVH = LbpMarkitClient.transfer(lastDVH, "<PrivateData/>", transferRecipientXML,
							"Transfert from API to " + markitGroupId);
					String response = "The deal has been successfully transferred, new DVH " + newDVH;
					return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(response));
				} else {
					return ResponseEntity.status(HttpStatus.OK)
							.body(new ResponseMessage(" No DVH found for the given trade"));
				}

			} catch (ErrorCode | Exception exception) {
				LOGGER.error(exception);
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				exception.printStackTrace(pw);
				String sStackTrace = sw.toString();
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sStackTrace));
			}
		}
	 
	 @RequestMapping(value = "/transferTov1", method = RequestMethod.GET)
		@ResponseBody
		public ResponseEntity<ResponseMessage> transferTov1(@RequestParam(value="calypsoTradeId") String calypsoTradeId,@RequestParam(value="calypsoMessageId") String calypsoMessageId,@RequestParam(value="markitDVH") String markitDVH,@RequestParam(value="comment") String comment) {
		 LOGGER.info("transferTo with calyspoTradeId: {0} and calypsoMessageId: {1} and DVH: {2} and comment: {3}",calypsoTradeId,calypsoMessageId,markitDVH,comment);
			try {
				String transferDealTeampleContent = new String(Files.readAllBytes(Paths.get(ApplicationProperties.LBP_MARKIT_TRANSFERDEAL_TEMPLATE_PATH)));
				transferDealTeampleContent.replace("##calypsoTradeId##", calypsoTradeId);
				transferDealTeampleContent.replace("##calypsoMessageId##", calypsoMessageId);
				transferDealTeampleContent.replace("##comment##", comment);
				transferDealTeampleContent.replace("##markitDVH##", markitDVH);
				String oldDealVersionHandle = Tools.getOldDealVersionHandle(transferDealTeampleContent);
				String privateDataXML = Tools.getPrivateData(transferDealTeampleContent);
				String recipientXML = Tools.getRecipienData(transferDealTeampleContent);
				String newDVH = LbpMarkitClient.transferDeal(oldDealVersionHandle, privateDataXML, recipientXML, comment);
				String response = "The deal has been successfully transferred, new DVH " + newDVH;
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(response));
			} catch (ErrorCode errorCode) {
				LOGGER.error(errorCode);
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(errorCode.errorString));
			} catch (NumberFormatException |IOException e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				String sStackTrace = sw.toString();
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sStackTrace));
			}
		}

		@RequestMapping(value = "/isconnected", method = RequestMethod.GET)
		@ResponseBody
		public ResponseEntity<ResponseMessage> isConnected() {
			LOGGER.debug("received session check");
			try {
				String result = LbpMarkitClient.IS_MARKIT_SESSION_CONNECTED ? "markitdealer session connected"
						: "markitdealer session not connected";
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(result));
			} catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				String sStackTrace = sw.toString();
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sStackTrace));
			}
		}

		@RequestMapping(value = "/loggedout", method = RequestMethod.GET)
		@ResponseBody
		public ResponseEntity<ResponseMessage> loggedout() {
			LOGGER.debug("received loggedout");
			try {
				markitApplication.stopMarkitConnector();
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("client is loggedout"));

			} catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				String sStackTrace = sw.toString();
				return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sStackTrace));
			}
		}

		@PreDestroy
		public void onExit() {
			LOGGER.info("Markit connector will be shutdown.");
			if (this.markitApplication != null) {
				markitApplication.stopMarkitConnector();
			}
		}
}
