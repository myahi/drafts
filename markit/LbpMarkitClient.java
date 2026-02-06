package fr.lbp.markit.controller;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.JMSException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.lbp.jms.connection.JMSListener;


import fr.lbp.markit.configuration.ApplicationProperties;
import fr.lbp.markit.connection.DealerAPIWrapper;
import fr.lbp.markit.connection.ErrorCode;
import fr.lbp.markit.connection.Session;

public class LbpMarkitClient {

    private static volatile LbpMarkitClient INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger(LbpMarkitClient.class);

    // ---- Locks
    private static final Object SESSION_LOCK = new Object();
    private static final Object EXECUTOR_LOCK = new Object();

    // ---- Etat global
    private static volatile Session MARKIT_SESSION;
    private static final AtomicInteger CURRENT_CONNECTION_ERROR_COUNT = new AtomicInteger(0);

    public static volatile boolean IS_MARKIT_SESSION_CONNECTED = false;
    public static volatile boolean DISCONNECTION_REQUEST_RECEIVED = false;


    private final int maxConnectionRetryCount = Integer.parseInt(ApplicationProperties.LBP_MARKIT_MAX_RETRY_COUNT);

    private static ExecutorService executorService;

    // ---- Non static (comme proposé)
    private JMSListener jmsListner;

    static {
        System.load(System.getenv("LD_LIBRARY_PATH_SO"));
    }

    public LbpMarkitClient() {
        INSTANCE = this;
        try {
            this.initializeMarkitConnector(false);
            DISCONNECTION_REQUEST_RECEIVED = false;
        } catch (JMSException | ErrorCode e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        return IS_MARKIT_SESSION_CONNECTED;
    }

    public boolean isMarkitSessionConnected() {
        Session s = getSession();
        return s != null && s.isValidClientSession();
    }

    public void initializeMarkitConnector(boolean resetRetryCount) throws JMSException, ErrorCode {
        LOGGER.info("initialize MarkitConnector with count error count: " + CURRENT_CONNECTION_ERROR_COUNT);

        if (resetRetryCount) {
            CURRENT_CONNECTION_ERROR_COUNT.set(0);
            DISCONNECTION_REQUEST_RECEIVED = false;
        }

        if (IS_MARKIT_SESSION_CONNECTED || DISCONNECTION_REQUEST_RECEIVED) {
            if (IS_MARKIT_SESSION_CONNECTED) {
                LOGGER.warn("Markit dealer connector already started");
            }
            return;
        }

        // 1 seul executor, protégé par lock
        synchronized (EXECUTOR_LOCK) {
            if (executorService == null || executorService.isShutdown()) {
                executorService = Executors.newSingleThreadExecutor();
            }
        }

        if(jmsListner == null) {
        	jmsListner = new JMSListener();
        }

        String[] libraryVersion = { new String() };
        LOGGER.info("SW_API_DLL Version = " + libraryVersion[0]);

        String markitHost = ApplicationProperties.LBP_MARKTI_HOST_NAME;
        String markitPort = ApplicationProperties.LBP_MARKIT_PORT;
        String markitUserName = ApplicationProperties.LBP_MARKIT_USER_NAME;
        String markitPassword = ApplicationProperties.LBP_MARKIT_PASSWORD;

        Session session = new Session(
                markitHost + ":" + markitPort,
                markitUserName,
                markitPassword,
                Integer.valueOf(ApplicationProperties.LBP_MARKIT_CONNECTION_TIMEOUT)
        );

        executorService.execute(() -> {
            // ✅ Retry DANS LE MEME THREAD
            while (!DISCONNECTION_REQUEST_RECEIVED && CURRENT_CONNECTION_ERROR_COUNT.get() < maxConnectionRetryCount) {
                try {
                    DealerAPIWrapper.poll(
                            Integer.valueOf(ApplicationProperties.LBP_MARKIT_CONNECTION_TESTDURATION),
                            Integer.valueOf(ApplicationProperties.LBP_MARKIT_CONNECTION_TIMEOUT)
                    );

                    // Healthcheck si déjà connecté
                    Session s = getSession();
                    if (s != null && IS_MARKIT_SESSION_CONNECTED && !DISCONNECTION_REQUEST_RECEIVED) {
                        String[] books = { new String() };
                        DealerAPIWrapper.getBookList(s.getLoginHandle(), books);
                        IS_MARKIT_SESSION_CONNECTED = true;
                        CURRENT_CONNECTION_ERROR_COUNT.set(0);
                    }

                    // Tentative de connexion si pas connecté
                    if (!IS_MARKIT_SESSION_CONNECTED && !DISCONNECTION_REQUEST_RECEIVED) {
                        session.deregisterCallbacks();
                        session.disconnect();
                        session.connect();

                        if (jmsListner != null) {
                            jmsListner.startListening();
                            session.setMessageHandler(jmsListner.getIncomingService());
                        }

                        setSession(session);
                        IS_MARKIT_SESSION_CONNECTED = true;
                        CURRENT_CONNECTION_ERROR_COUNT.set(0);
                    }

                } catch (ErrorCode err) {
                    handleConnectionIssue(err);

                    if (!DISCONNECTION_REQUEST_RECEIVED
                            && CURRENT_CONNECTION_ERROR_COUNT.get() < maxConnectionRetryCount) {
                        try {
                            Thread.sleep(5000L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }

                }

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }

            // Si on sort parce qu'on a épuisé les retries
            if (CURRENT_CONNECTION_ERROR_COUNT.get() >= maxConnectionRetryCount) {
                    stopMarkitConnectorInternal();
            }
        });
    }

    private void handleConnectionIssue(ErrorCode err) {
        int attempt = CURRENT_CONNECTION_ERROR_COUNT.incrementAndGet();

        LOGGER.error("Exception occurred while trying connection retry attempt {}", attempt);
        LOGGER.error(err);

        IS_MARKIT_SESSION_CONNECTED = false;

        if (attempt >= maxConnectionRetryCount) {
            LOGGER.error("Maximum try number has been reached");
            
                stopMarkitConnectorInternal();

                Date currentDate = new Date();
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                if (jmsListner != null) {
                	jmsListner.send(JMSListener.OutgoingType.FUNCTIONAL, dateFormat.format(currentDate) + ": Le nombre de tentative de connexion a été atteint sur le connecteur markitdealer\n le connecteur a été arrété.");
                }
            } 
         else {
            LOGGER.info("Current connection retry count: " + attempt);
            LOGGER.info("Max connection retry count: " + maxConnectionRetryCount);
        }
    }

    public static void loggedOut() throws ErrorCode {
        Session s = getSession();
        if (s != null) {
            DealerAPIWrapper.logout(s.getLoginHandle());
        }
        DISCONNECTION_REQUEST_RECEIVED = true;
        IS_MARKIT_SESSION_CONNECTED = false;
    }

    // API statique conservée
    public void stopMarkitConnector() {
        if (INSTANCE != null) {
            INSTANCE.stopMarkitConnectorInternal();
        } else {
            // fallback minimal
            DISCONNECTION_REQUEST_RECEIVED = true;
            IS_MARKIT_SESSION_CONNECTED = false;
            synchronized (EXECUTOR_LOCK) {
                if (executorService != null) {
                    executorService.shutdownNow();
                    try {
                        executorService.awaitTermination(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    executorService = null;
                }
            }
        }
    }

    private void stopMarkitConnectorInternal(){
        try {
            DISCONNECTION_REQUEST_RECEIVED = true;
            try {
            	if (jmsListner != null) {
            		jmsListner.stopListening();
            	}
            	Session session = getSession();
            	if (session != null) {
            		DealerAPIWrapper.logout(session.getLoginHandle());
            		IS_MARKIT_SESSION_CONNECTED = false;
            		LOGGER.info("Session logout");
            	}
            } catch (Exception | ErrorCode exception) {
            	LOGGER.error(exception);
            }
            
            IS_MARKIT_SESSION_CONNECTED = false;
            synchronized (EXECUTOR_LOCK) {
                if (executorService != null) {
                    executorService.shutdownNow();
                    executorService.awaitTermination(5, TimeUnit.SECONDS);
                    executorService = null;
                }
            }
            LOGGER.info("Markit connection stopped");
        } catch (InterruptedException error) {
            LOGGER.error(error);
            Thread.currentThread().interrupt();
        }
    }
    public synchronized void restartConnector() throws ErrorCode {
        try {
            // stop propre si un thread tourne déjà
            try {
            	if(IS_MARKIT_SESSION_CONNECTED){
            		stopMarkitConnector();            		
            	}
            } catch (Exception ignore) {
                // ignore
            }

            // reset état (important)
            DISCONNECTION_REQUEST_RECEIVED = false;
            IS_MARKIT_SESSION_CONNECTED = false;
            // si tu as AtomicInteger :
            CURRENT_CONNECTION_ERROR_COUNT.set(0);

            // relance avec resetRetryCount=true
            initializeMarkitConnector(true);

            LOGGER.info("Restart Markit connection  in progress...");
        } catch (Exception e) {
            LOGGER.error("restartConnector failed", e);
            throw new RuntimeException("restartConnector failed: " + e.getMessage(), e);
        }
    }

    // ----------------------------
    // Session access thread-safe
    // ----------------------------
    private static Session getSession() {
        synchronized (SESSION_LOCK) {
            return MARKIT_SESSION;
        }
    }

    private static void setSession(Session s) {
        synchronized (SESSION_LOCK) {
            MARKIT_SESSION = s;
        }
    }

    // --------------------------------------------------------------------
    // Méthodes métiers : signatures inchangées, session lue thread-safe
    // --------------------------------------------------------------------

    public static String[] getAllDealVersionHandles(String tradeId, String contractVersion, String side)
            throws NumberFormatException, ErrorCode {
        String[] dealVersionHandles = { new String() };
        Session s = getSession();
        if (s == null) return dealVersionHandles;
        DealerAPIWrapper.getAllDealVersionHandles(s.getLoginHandle(),
                Long.valueOf(tradeId),
                Integer.valueOf(contractVersion),
                Integer.valueOf(side),
                dealVersionHandles);
        return dealVersionHandles;
    }

    public static String getSWDML(String dealVersionHandle, String privateDataXML, String recipientXML, String messageText)
            throws IOException, ErrorCode {
        String[] newDealVersionHandle = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.getDealSWDML(s.getLoginHandle(), "SWDML_4_2: 4.2", dealVersionHandle, newDealVersionHandle);
            LOGGER.info("Deal Submitted To Markit " + newDealVersionHandle[0]);
            return newDealVersionHandle[0];
        }
        return null;
    }

    public static String submitNewDeal(String swdml, String privateDataXML, String recipientXML, String messageText)
            throws IOException, ErrorCode {
        String[] newDealVersionHandle = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.submitNewDeal(s.getLoginHandle(), swdml, privateDataXML, recipientXML, messageText, newDealVersionHandle);
            LOGGER.info("Deal Submitted To Markit " + newDealVersionHandle[0]);
            return newDealVersionHandle[0];
        }
        return null;
    }

    public static String submitDraftNewDeal(String swdml, String privateDataXML, String recipientXML, String messageText)
            throws IOException, ErrorCode {
        String[] newDealVersionHandle = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.submitDraftNewDeal(s.getLoginHandle(), swdml, privateDataXML, recipientXML, messageText, newDealVersionHandle);
            LOGGER.info("Draft deal submitted To Markit " + newDealVersionHandle[0]);
            return newDealVersionHandle[0];
        }
        return null;
    }

    public static String releaseDeal(String privateDataXML, String oldDealVersionHandle) throws IOException, ErrorCode {
        String[] newDealVersionHandle = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.release(s.getLoginHandle(), oldDealVersionHandle, privateDataXML, newDealVersionHandle);
            return newDealVersionHandle[0];
        }
        return null;
    }

    public static String cancelDeal(String privateDataXML, String oldDealVersionHandle) throws IOException, ErrorCode {
        String[] newDealVersionHandle = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.release(s.getLoginHandle(), oldDealVersionHandle, privateDataXML, newDealVersionHandle);
            return newDealVersionHandle[0];
        }
        return null;
    }

    public static String rejectDeal(String oldDealVersionHandle, String messageText) throws IOException, ErrorCode {
        String[] newDealVersionHandle = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.rejectDK(s.getLoginHandle(), oldDealVersionHandle, messageText, newDealVersionHandle);
            return newDealVersionHandle[0];
        }
        return null;
    }

    public static String transferDeal(String oldDealVersionHandle, String privateDataXML, String recipientXML, String messageText)
            throws IOException, ErrorCode {
        String[] newDealVersionHandle = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.transfer(s.getLoginHandle(), oldDealVersionHandle, privateDataXML, recipientXML, messageText, newDealVersionHandle);
            return newDealVersionHandle[0];
        }
        return null;
    }

    public static String updateDeal(String oldDealVersionHandle, String sinkUpdateXML) throws IOException, ErrorCode {
        String[] newDealVersionHandle = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.update(s.getLoginHandle(), oldDealVersionHandle, sinkUpdateXML, newDealVersionHandle);
            return newDealVersionHandle[0];
        }
        return null;
    }

    public static String submitPostTradeEvent(String privateDataXML, String oldDealVersionHandle, String postTradeXML,
            String recipientXML, String messageText) throws IOException, ErrorCode {
        String[] newDealVersionHandle = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.submitPostTradeEvent(s.getLoginHandle(), oldDealVersionHandle, postTradeXML, privateDataXML, recipientXML, messageText, newDealVersionHandle);
            return newDealVersionHandle[0];
        }
        return null;
    }

    public static String pickUpDeal(String oldDealVersionHandle, String privateDataXML) throws IOException, ErrorCode {
        String[] newDealVersionHandleForPickUp = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.pickup(s.getLoginHandle(), oldDealVersionHandle, privateDataXML, newDealVersionHandleForPickUp);
            return newDealVersionHandleForPickUp[0];
        }
        return null;
    }

    public static String affirmDeal(String oldDealVersionHandle, String privateDataXML, String swdml) throws IOException, ErrorCode {
        String[] newDealVersionHandleForAffirm = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            LOGGER.info("Sending affirm for DVH:  " + oldDealVersionHandle);
            LOGGER.info("Sending affirm with PrivateDataXML:  " + privateDataXML);
            LOGGER.info("Sending affirm with swdml:  " + swdml);
            DealerAPIWrapper.affirm(s.getLoginHandle(), oldDealVersionHandle, swdml, privateDataXML, "AFFIRM FROM API CLIENT", newDealVersionHandleForAffirm);
            return newDealVersionHandleForAffirm[0];
        }
        return null;
    }

    public static String acceptAffirmDeal(String oldDealVersionHandle, String privateDataXML) throws IOException, ErrorCode {
        String[] newDealVersionHandleForAcceptAffirm = { new String() };
        Session s = getSession();
        if (s != null && s.isValidClientSession()) {
            DealerAPIWrapper.acceptAffirm(s.getLoginHandle(), oldDealVersionHandle, privateDataXML, newDealVersionHandleForAcceptAffirm);
            return newDealVersionHandleForAcceptAffirm[0];
        }
        return null;
    }

    public static String transfer(String oldDealVersionHandle, String privateDataXML, String transferRecipientXML, String messageText)
            throws IOException, ErrorCode {
        String[] newDealVersionHandle = { new String() };
        Session s = getSession();
        if (s == null) return null;
        DealerAPIWrapper.transfer(s.getLoginHandle(), oldDealVersionHandle, privateDataXML, transferRecipientXML, messageText, newDealVersionHandle);
        LOGGER.info("Draft deal submitted To Markit " + newDealVersionHandle[0]);
        return newDealVersionHandle[0];
    }

    public static String[] getMyInterestGroups() throws IOException, ErrorCode {
        String[] resultXML = { new String() };
        Session s = getSession();
        if (s == null) return resultXML;
        DealerAPIWrapper.getMyInterestGroups(s.getLoginHandle(), resultXML);
        LOGGER.info("Get my InterestGroups " + String.join(";", resultXML));
        return resultXML;
    }

    public static String[] getMyUserInfo() throws IOException, ErrorCode {
        String[] resultXML = { new String() };
        Session s = getSession();
        if (s == null) return resultXML;
        DealerAPIWrapper.getMyuserInfo(s.getLoginHandle(), resultXML);
        LOGGER.info("Get my user informations " + String.join(";", resultXML));
        return resultXML;
    }

    public static int getDealGetMySide(long dealId) throws IOException, ErrorCode {
        int[] side_out = { Integer.valueOf(1) };
        Session s = getSession();
        if (s == null) return side_out[0];
        DealerAPIWrapper.getDealGetMySide(s.getLoginHandle(), dealId, side_out);
        LOGGER.info("getDealGetMySide for trade Id : " + dealId + " " + side_out[0]);
        return side_out[0];
    }

    public static String[] queryDeals(String queryXML) throws IOException, ErrorCode {
        String[] resultsXML = { new String() };
        Session s = getSession();
        if (s == null) return resultsXML;
        DealerAPIWrapper.queryDeals(s.getLoginHandle(), queryXML, resultsXML);
        for (String result : resultsXML) {
            LOGGER.info("Query deals result: " + result);
        }
        return resultsXML;
    }

    public static String getDealSWML(String dealVersionHandle) throws ErrorCode {
        String[] swdml = { new String() };
        Session s = getSession();
        if (s == null) return null;
        DealerAPIWrapper.getDealSWML(s.getLoginHandle(), "4.2", dealVersionHandle, swdml);
        return swdml[0];
    }

    public static String getDealSWDML(String dealVersionHandle) throws ErrorCode {
        String[] swdml = { new String() };
        Session s = getSession();
        if (s == null) return null;
        DealerAPIWrapper.getDealSWDML(s.getLoginHandle(), "4.2", dealVersionHandle, swdml);
        return swdml[0];
    }

    public static String withdrawDeal(String dealVersionHandle, String privateDataXML) throws ErrorCode {
        Session s = getSession();
        if (s == null) return null;

        String[] newDealVersionHandleForPull = { new String() };
        DealerAPIWrapper.pull(s.getLoginHandle(), dealVersionHandle, newDealVersionHandleForPull);

        String[] newDealVersionHandleForWithdraw = { new String() };
        DealerAPIWrapper.withdraw(s.getLoginHandle(), newDealVersionHandleForPull[0], newDealVersionHandleForWithdraw);
        return newDealVersionHandleForWithdraw[0];
    }
}
