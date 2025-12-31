package fr.lbp.jms.connection;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.swapswire.sw_api.SWAPILinkModuleConstants;

import fr.lbp.markit.connection.ErrorCode;
import fr.lbp.markit.controller.LbpMarkitClient;
import fr.lbp.markit.tools.Tools;

public class MarkitIncomingService {

    private static final Logger LOGGER = LogManager.getLogger(MarkitIncomingService.class);

    // Le métier ne connaît PAS les queues, seulement un sender sémantique (type + payload)
    private final EmsJmsListener jms;

    public MarkitIncomingService(EmsJmsListener jms) {
        this.jms = jms;
    }

    public void handle(String messageContent) throws IOException, ErrorCode {
        String inputMessageReference = null;

        try {
            String markitAction = Tools.getMarkitActionType(messageContent);
            LOGGER.info("MARKIT_DEALER ACTION >>> {}", markitAction);

            inputMessageReference = Tools.getSourceRefMessage(messageContent);

            String swdml = Tools.getSWDML(messageContent);
            String privateData = Tools.getPrivateData(messageContent);
            String recipientData = Tools.getRecipienData(messageContent);
            String sinkUpdateData = Tools.getSinkUpdateData(messageContent);
            String messageTextData = Tools.getMessageText(messageContent);
            String oldDealVersionHandle = Tools.getOldDealVersionHandle(messageContent);
            String currentDate = Tools.getCurrentTimeStamp();

            if (Tools.MARKIT_ACTION_NEW.equals(markitAction)) {
                LOGGER.info("SubmitNewDeal ref >>> {}", inputMessageReference);
                String dvh = LbpMarkitClient.submitNewDeal(swdml, privateData, recipientData, messageTextData);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_DRAFT_NEW.equals(markitAction)) {
                LOGGER.info("SubmitDraftNewDeal ref >>> {}", inputMessageReference);
                String dvh = LbpMarkitClient.submitDraftNewDeal(swdml, privateData, recipientData, messageTextData);
                Tools.updateLastExchangeTimeStamp(currentDate);

                String outgoing = Tools.buildReponseToSourceForDraft(inputMessageReference, dvh);
                jms.send(EmsJmsListener.OutgoingType.FUNCTIONAL, outgoing);

            } else if (Tools.MARKIT_ACTION_DRAFT_AND_TRANSFERT.equals(markitAction)) {
                LOGGER.info("SubmitDraftNewDealAndTransfert ref >>> {}", inputMessageReference);

                String dvh = LbpMarkitClient.submitDraftNewDeal(swdml, privateData, recipientData, messageTextData);
                String transferRecipientXML = Tools.getSecondRecipienData(messageContent);

                LbpMarkitClient.transferDeal(dvh, privateData, transferRecipientXML,
                        "TRANSFERT DRAFT FROM API USER TO MARKIT USER");

            } else if (Tools.MARKIT_ACTION_RELEASE.equals(markitAction)) {
                LOGGER.info("ReleaseDeal ref >>> {}", inputMessageReference);
                String dvh = LbpMarkitClient.releaseDeal(privateData, oldDealVersionHandle);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_PICKUP.equals(markitAction)) {
                LOGGER.info("PickUpDeal ref >>> {}", inputMessageReference);
                String dvh = LbpMarkitClient.pickUpDeal(oldDealVersionHandle, privateData);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_AFFIRM.equals(markitAction)) {
                String swdmlForAffirm = Tools.getSWDMLForAffirm(messageContent);
                LOGGER.info("AffirmDeal ref >>> {}", inputMessageReference);
                String dvh = LbpMarkitClient.affirmDeal(oldDealVersionHandle, privateData, swdmlForAffirm);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_WITHDRAW.equals(markitAction)) {
                LOGGER.info("WithdrawDeal ref >>> {}", inputMessageReference);
                String dvh = LbpMarkitClient.withdrawDeal(oldDealVersionHandle, privateData);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_ACCEPT_AFFIRM.equals(markitAction)) {
                LOGGER.info("AcceptAffirmDeal ref >>> {}", inputMessageReference);
                String dvh = LbpMarkitClient.acceptAffirmDeal(oldDealVersionHandle, privateData);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_CANCELLATION.equals(markitAction)) {
                LOGGER.info("Cancellation ref >>> {}", inputMessageReference);
                String postTradeXML = Tools.getCancellationPart(messageContent);
                String dvh = LbpMarkitClient.submitPostTradeEvent(privateData, oldDealVersionHandle, postTradeXML, recipientData, messageTextData);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_PARTIAL_TERMANATE.equals(markitAction)) {
                LOGGER.info("Partial terminate ref >>> {}", inputMessageReference);
                String postTradeXML = Tools.getPartialTerminatePart(messageContent);
                String dvh = LbpMarkitClient.submitPostTradeEvent(privateData, oldDealVersionHandle, postTradeXML, recipientData, messageTextData);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_EXERCISE.equals(markitAction)) {
                LOGGER.info("Exercise ref >>> {}", inputMessageReference);
                String postTradeXML = Tools.getExercisePart(messageContent);
                String dvh = LbpMarkitClient.submitPostTradeEvent(privateData, oldDealVersionHandle, postTradeXML, recipientData, messageTextData);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_REJECT.equals(markitAction)) {
                LOGGER.info("Reject ref >>> {}", inputMessageReference);
                String dvh = LbpMarkitClient.rejectDeal(oldDealVersionHandle, messageTextData);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_TRANSFER.equals(markitAction)) {
                LOGGER.info("Transfer ref >>> {}", inputMessageReference);
                String dvh = LbpMarkitClient.transferDeal(oldDealVersionHandle, privateData, recipientData, messageTextData);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else if (Tools.MARKIT_ACTION_UPDATE.equals(markitAction)) {
                LOGGER.info("Update ref >>> {}", inputMessageReference);
                String dvh = LbpMarkitClient.updateDeal(oldDealVersionHandle, sinkUpdateData);
                Tools.updateLastExchangeTimeStamp(currentDate);
                LOGGER.info("DVH received >>> {} for {}", dvh, inputMessageReference);

            } else {
                LOGGER.error("Unhandled action Markit for ref >>> {}", inputMessageReference);
            }

        } catch (ErrorCode e) {
            int errorCode = e.errorCode;

            if (isConnectionError(errorCode)) {
                jms.send(EmsJmsListener.OutgoingType.TECHNICAL_ERROR, e.getMessage());
            } else {
                String outgoing = Tools.buildErrorToSource(inputMessageReference, String.valueOf(errorCode), e.toString());
                jms.send(EmsJmsListener.OutgoingType.FUNCTIONAL_ERROR, outgoing);
            }

            throw e;

        } catch (Exception e) {
            String outgoing = Tools.buildErrorToSource(inputMessageReference, "", e.toString());
            jms.send(EmsJmsListener.OutgoingType.FUNCTIONAL_ERROR, outgoing);
            throw e;
        }
    }

    private boolean isConnectionError(int errorCode) {
        return errorCode == SWAPILinkModuleConstants.SWERR_InvalidHandle
                || errorCode == SWAPILinkModuleConstants.SWERR_LostConnection
                || errorCode == SWAPILinkModuleConstants.SWERR_UserLoggedOut
                || errorCode == SWAPILinkModuleConstants.SWERR_PasswordExpired
                || errorCode == SWAPILinkModuleConstants.SWERR_LoginLimitReached
                || errorCode == SWAPILinkModuleConstants.SWERR_AccountLocked
                || errorCode == SWAPILinkModuleConstants.SWERR_Timeout;
    }
}
