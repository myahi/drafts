package fr.lbp.markit.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.swapswire.sw_api.SW_DealNotifyData;

import fr.lbp.markit.configuration.ApplicationProperties;

public class Tools {
    private static final Logger LOGGER = LogManager.getLogger(Tools.class);

    public static final String MARKIT_ACTION_NEW = "NEW";
    public static final String MARKIT_ACTION_DRAFT_NEW = "DRAFT_NEW";
    public static final String MARKIT_ACTION_DRAFT_AMEND = "DRAFT_AMEND";
    public static final String MARKIT_ACTION_DRAFT_CANCEL = "DRAFT_CANCEL";
    public static final String MARKIT_ACTION_RELEASE = "RELEASE";
    public static final String MARKIT_ACTION_AFFIRM = "AFFIRM";
    public static final String MARKIT_ACTION_ACCEPT_AFFIRM = "ACCEPT_AFFIRM";
    public static final String MARKIT_ACTION_PARTIAL_TERMANATE = "PARTIAL_TERMANATE";
    public static final String MARKIT_ACTION_PICKUP = "PICKUP";
    public static final String MARKIT_ACTION_WITHDRAW = "WITHDRAW";
    public static final String MARKIT_ACTION_CANCELLATION = "CANCELLATION";
    public static final String MARKIT_ACTION_EXERCISE = "EXERCISE";
    public static final String MARKIT_ACTION_REJECT = "REJECT";
    public static final String MARKIT_ACTION_TRANSFER = "TRANSFER";
    public static final String MARKIT_ACTION_UPDATE = "UPDATE";
    public static final String MARKIT_ACTION_DRAFT_AND_TRANSFERT = "DRAFT_NEW_AND_TRANSFERT";

    public static String extractStackTraceExcetpionInoString(Throwable ex) {
        if (ex == null) return null;
        StringWriter stackTrace = new StringWriter();
        ex.printStackTrace(new PrintWriter(stackTrace));
        return stackTrace.toString();
    }

    public static String buildErrorMessage(String exception, String fixMessage) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<exception>").append(exception == null ? "" : escapeXml(exception)).append("</exception>");
        buffer.append("<fixMessage>").append(fixMessage == null ? "" : escapeXml(fixMessage)).append("</fixMessage>");
        return buffer.toString();
    }

    public static String getMarkitActionType(String messageContent) {
        if (messageContent == null) return null;

        String inputAction = StringUtils.substringBetween(messageContent, "<marketAction>", "</marketAction>");
        if (inputAction == null) return null;

        inputAction = inputAction.trim();

        if ("<actionName>NEW</actionName>".equals(inputAction)) return MARKIT_ACTION_NEW;
        if ("<actionName>DRAFT_NEW</actionName>".equals(inputAction)) return MARKIT_ACTION_DRAFT_NEW;
        if ("<actionName>RELEASE</actionName>".equals(inputAction)) return MARKIT_ACTION_RELEASE;
        if ("<actionName>AFFIRM</actionName>".equals(inputAction)) return MARKIT_ACTION_AFFIRM;
        if ("<actionName>PICKUP</actionName>".equals(inputAction)) return MARKIT_ACTION_PICKUP;
        if ("<actionName>WITHDRAW</actionName>".equals(inputAction)) return MARKIT_ACTION_WITHDRAW;
        if ("<actionName>CANCELLATION</actionName>".equals(inputAction)) return MARKIT_ACTION_CANCELLATION;
        if ("<actionName>EXERCISE</actionName>".equals(inputAction)) return MARKIT_ACTION_EXERCISE;
        if ("<actionName>ACCEPT_AFFIRM</actionName>".equals(inputAction)) return MARKIT_ACTION_ACCEPT_AFFIRM;
        if ("<actionName>BILATERAL_AMEND</actionName>".equals(inputAction)) return MARKIT_ACTION_PARTIAL_TERMANATE;
        if ("<actionName>REJECT</actionName>".equals(inputAction)) return MARKIT_ACTION_REJECT;
        if ("<actionName>TRANSFER</actionName>".equals(inputAction)) return MARKIT_ACTION_TRANSFER;
        if ("<actionName>UPDATE</actionName>".equals(inputAction)) return MARKIT_ACTION_UPDATE;
        if ("<actionName>DRAFT_NEW_AND_TRANSFERT</actionName>".equals(inputAction)) return MARKIT_ACTION_DRAFT_AND_TRANSFERT;

        return null;
    }

    public static String getSWDML(String messageContent) {
        if (messageContent == null) return null;
        String inputSWDML = StringUtils.substringBetween(messageContent, "<swdml>", "</swdml>");
        if (inputSWDML == null) return null;
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + inputSWDML;
    }

    public static String getSWDMLForAffirm(String messageContent) {
        if (messageContent == null) return null;

        String inputSWDML = StringUtils.substringBetween(messageContent, "<swdml>", "</swdml>");
        if (inputSWDML == null) return null;

        inputSWDML = StringUtils.replace(inputSWDML, "&lt;", "<");
        inputSWDML = StringUtils.replace(inputSWDML, "&gt;", ">");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + inputSWDML;
    }

    public static String getPrivateData(String messageContent) {
        if (messageContent == null) return null;
        return StringUtils.substringBetween(messageContent, "<PrivateDataData>", "</PrivateDataData>");
    }

    public static String getCancellationPart(String messageContent) {
        if (messageContent == null) return null;
        return StringUtils.substringBetween(messageContent, "<cancellationPart>", "</cancellationPart>");
    }

    public static String getPartialTerminatePart(String messageContent) {
        if (messageContent == null) return null;
        String inputPartialTerm = StringUtils.substringBetween(messageContent, "<swdml>", "</swdml>");
        return inputPartialTerm == null ? null : inputPartialTerm.trim();
    }

    public static String getExercisePart(String messageContent) {
        if (messageContent == null) return null;
        return StringUtils.substringBetween(messageContent, "<exercisePart>", "</exercisePart>");
    }

    public static String getRecipienData(String messageContent) {
        if (messageContent == null) return null;

        String recipientData = StringUtils.substringBetween(messageContent, "<RecipientData>", "</RecipientData>");
        if (recipientData == null) return null;

        if (recipientData.contains("<Recipient id=\"two\">")) {
            String part = StringUtils.substringBetween(recipientData, "<Recipient id=\"one\">", "<Recipient id=\"two\">");
            if (part == null) return null;

            String recipientOne = "<Recipient id=\"one\">" + part;
            if (recipientOne.contains("<UserId/>")) return null;

            return recipientOne;
        }

        return recipientData;
    }

    public static String getSecondRecipienData(String messageContent) {
        if (messageContent == null) return null;

        String recipientData = StringUtils.substringBetween(messageContent, "<RecipientData>", "</RecipientData>");
        if (recipientData == null) return null;

        if (recipientData.contains("<Recipient id=\"two\">")) {
            String part = StringUtils.substringBetween(recipientData, "<Recipient id=\"two\">", "</Recipient>");
            if (part == null) return null;
            return "<Recipient id=\"one\">" + part + "</Recipient>";
        }

        return recipientData;
    }

    public static String getSinkUpdateData(String messageContent) {
        if (messageContent == null) return null;
        return StringUtils.substringBetween(messageContent, "<SinkUpdatePart>", "</SinkUpdatePart>");
    }

    public static String getOldDealVersionHandle(String messageContent) {
        if (messageContent == null) return null;
        return StringUtils.substringBetween(messageContent, "<dvh>", "</dvh>");
    }

    public static String getMessageText(String messageContent) {
        if (messageContent == null) return null;
        return StringUtils.substringBetween(messageContent, "<messageTextContent>", "</messageTextContent>");
    }

    public static String getSourceRefMessage(String messageContent) {
        if (messageContent == null) return null;
        return StringUtils.substringBetween(messageContent, "<swAdditionalField sequence=\"1\">", "</swAdditionalField>");
    }

    public static String buildResponseToSource(SW_DealNotifyData dnData, String swml) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<root><swmlDealNotification>");
        buffer.append("<LH>").append(escapeXml(String.valueOf(dnData.getLh()))).append("</LH>");
        buffer.append("<BrokerDealId>").append(escapeXml(dnData.getBrokerId())).append("</BrokerDealId>");
        buffer.append("<DealID>").append(escapeXml(dnData.getDealId())).append("</DealID>");
        buffer.append("<MajorVersion>").append(escapeXml(dnData.getMajorVer())).append("</MajorVersion>");
        buffer.append("<MinorVersion>").append(escapeXml(dnData.getMinorVer())).append("</MinorVersion>");
        buffer.append("<PrivateVersion>").append(escapeXml(dnData.getPrivateVer())).append("</PrivateVersion>");
        buffer.append("<Side>").append(escapeXml(dnData.getSide())).append("</Side>");
        buffer.append("<PreviousDVH>").append(escapeXml(dnData.getPrevDVH())).append("</PreviousDVH>");
        buffer.append("<DVH>").append(escapeXml(dnData.getDvh())).append("</DVH>");
        buffer.append("<NewState>").append(escapeXml(dnData.getNewState())).append("</NewState>");
        buffer.append("<NewStateString>").append(escapeXml(dnData.getNewStateStr())).append("</NewStateString>");
        buffer.append("<ContractState>").append(escapeXml(dnData.getContractState())).append("</ContractState>");
        buffer.append("<ProductType>").append(escapeXml(dnData.getProductType())).append("</ProductType>");
        buffer.append("<TradeAttrFlags>").append(escapeXml(dnData.getTradeAttrFlags())).append("</TradeAttrFlags>");
        buffer.append("<refMessageId>").append("</refMessageId>");
        buffer.append("</swmlDealNotification>");
        buffer.append("<swml>").append(swml == null ? "" : escapeXml(swml)).append("</swml>");
        buffer.append("</root>");
        return buffer.toString();
    }

    public static String buildErrorToSource(String ref, String errorCode, String stackTrace) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<root>");
        buffer.append("<messageId>").append(ref == null ? "" : escapeXml(ref)).append("</messageId>");
        buffer.append("<errorCode>").append(errorCode == null ? "" : escapeXml(errorCode)).append("</errorCode>");
        buffer.append("<stackTrace>").append(stackTrace == null ? "" : escapeXml(stackTrace)).append("</stackTrace>");
        buffer.append("</root>");
        return buffer.toString();
    }

    public static String buildReponseToSourceForDraft(String ref, String dvh) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<root><swmlDealNotification>");
        buffer.append("<LH>").append("</LH>");
        buffer.append("<BrokerDealId>").append("</BrokerDealId>");
        buffer.append("<DealID>").append("</DealID>");
        buffer.append("<MajorVersion>").append("</MajorVersion>");
        buffer.append("<MinorVersion>").append("</MinorVersion>");
        buffer.append("<PrivateVersion>").append("</PrivateVersion>");
        buffer.append("<Side>").append("</Side>");
        buffer.append("<PreviousDVH>").append("</PreviousDVH>");
        buffer.append("<DVH>").append(dvh == null ? "" : escapeXml(dvh)).append("</DVH>");
        buffer.append("<NewState>").append("</NewState>");
        buffer.append("<NewStateString>").append("</NewStateString>");
        buffer.append("<ContractState>").append("</ContractState>");
        buffer.append("<ProductType>").append("</ProductType>");
        buffer.append("<TradeAttrFlags>").append("</TradeAttrFlags>");
        buffer.append("<refMessageId>").append(ref == null ? "" : escapeXml(ref)).append("</refMessageId>");
        buffer.append("</swmlDealNotification>");
        buffer.append("<swml>").append("</swml>");
        buffer.append("</root>");
        return buffer.toString();
    }

    public static String getCurrentTimeStamp() {
        Instant instant = Instant.now();
        ZoneId zone = ZoneId.of("GMT");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        return instant.atZone(zone).format(formatter);
    }

    public static void updateLastExchangeTimeStamp(String exchangeTimeStamp) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ApplicationProperties.LBP_MARKIT_LAST_EXCHANGE_DATE))) {
            bw.write(exchangeTimeStamp == null ? "" : exchangeTimeStamp);
            LOGGER.info("Last reception date updated >> {}", exchangeTimeStamp);
        } catch (IOException e) {
            LOGGER.error("Failed to update last exchange timestamp", e);
        }
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
