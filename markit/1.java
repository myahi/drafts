
2026-02-06 18:22:00.468 ERROR 75573 --- [pool-2-thread-1] f.lbp.markit.controller.LbpMarkitClient  : ApplicationContext not set: fallback System.exit(2)
    

int threshold = Integer.parseInt(ApplicationProperties.LBP_MARKIT_EMAIL_ALERT_THRESHOLD);

if (attempt >= threshold && EMAIL_SENT.compareAndSet(false, true)) {
    mailService.send(...);
}


package fr.lbp.markit.controller;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.jms.JMSException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.swapswire.sw_api.SWAPILinkModule;

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

    // ---- Non static
    private JMSListener jmsListner;

    // ✅ éviter shutdown/await depuis le thread worker
    private static volatile Thread MARKIT_WORKER_THREAD;

    // ✅ éviter spam d’alerte
    private static final AtomicBoolean MAX_RETRY_ALERT_SENT = new AtomicBoolean(false);

    // ✅ pour arrêter Spring proprement quand max retry atteint
    private static volatile ConfigurableApplicationContext APP_CONTEXT;

    static {
        System.load(System.getenv("LD_LIBRARY_PATH_SO"));
    }

    /**
     * A appeler depuis le main :
     *   ConfigurableApplicationContext ctx = SpringApplication.run(...);
     *   LbpMarkitClient.setApplicationContext(ctx);
     */
    public static void setApplicationContext(ConfigurableApplicationContext ctx) {
        APP_CONTEXT = ctx;
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
            MAX_RETRY_ALERT_SENT.set(false);
        }

        if (IS_MARKIT_SESSION_CONNECTED || DISCONNECTION_REQUEST_RECEIVED) {
            if (IS_MARKIT_SESSION_CONNECTED) {
                LOGGER.warn("Markit dealer connector already started");
            }
            return;
        }

        synchronized (EXECUTOR_LOCK) {
            if (executorService == null || executorService.isShutdown()) {
                executorService = Executors.newSingleThreadExecutor();
            }
        }

        if (jmsListner == null) {
            jmsListner = new JMSListener();
        }

        String[] libraryVersion = { new String() };
        LOGGER.info("SW_API_DLL Version = " + libraryVersion[0]);

        final String markitHost = ApplicationProperties.LBP_MARKTI_HOST_NAME;
        final String markitPort = ApplicationProperties.LBP_MARKIT_PORT;
        final String markitUserName = ApplicationProperties.LBP_MARKIT_USER_NAME;
        final String markitPassword = ApplicationProperties.LBP_MARKIT_PASSWORD;
        final int timeoutMs = Integer.valueOf(ApplicationProperties.LBP_MARKIT_CONNECTION_TIMEOUT);

        // ✅ recrée une Session clean (corrige -301 cookie rejected / handles pourris)
        final Supplier<Session> sessionFactory = () -> new Session(
                markitHost + ":" + markitPort,
                markitUserName,
                markitPassword,
                timeoutMs
        );

        executorService.execute(() -> {
            MARKIT_WORKER_THREAD = Thread.currentThread();

            Session currentSession = sessionFactory.get();

            while (!DISCONNECTION_REQUEST_RECEIVED
                    && CURRENT_CONNECTION_ERROR_COUNT.get() < maxConnectionRetryCount) {

                try {
                    // poll = “tick” (garde ce que tu as déjà)
                    DealerAPIWrapper.poll(
                            Integer.valueOf(ApplicationProperties.LBP_MARKIT_CONNECTION_TESTDURATION),
                            Integer.valueOf(ApplicationProperties.LBP_MARKIT_CONNECTION_TIMEOUT)
                    );

                    // Healthcheck si déjà connecté (appel API authentifié)
                    Session s = getSession();
                    if (s != null && IS_MARKIT_SESSION_CONNECTED && !DISCONNECTION_REQUEST_RECEIVED) {
                        String[] books = { new String() };
                        DealerAPIWrapper.getBookList(s.getLoginHandle(), books);

                        IS_MARKIT_SESSION_CONNECTED = true;
                        CURRENT_CONNECTION_ERROR_COUNT.set(0);
                        MAX_RETRY_ALERT_SENT.set(false);
                    }

                    // Tentative de connexion si pas connecté
                    if (!IS_MARKIT_SESSION_CONNECTED && !DISCONNECTION_REQUEST_RECEIVED) {

                        // ✅ cleanup best-effort (sans doubler deregister -> évite double log)
                        try { currentSession.disconnect(); } catch (Exception | ErrorCode ignore) {}

                        // (re)connect
                        currentSession.connect();

                        // ✅ ne jamais déclarer connected si la session applicative n’est pas valide
                        if (!currentSession.isValidClientSession()) {
                            LOGGER.warn("Session invalid after connect/login: forcing retry");
                            throw new ErrorCode(SWAPILinkModule.SWERR_Timeout);
                        }

                        // handler + JMS
                        if (jmsListner != null) {
                            jmsListner.startListening();
                            currentSession.setMessageHandler(jmsListner.getIncomingService());
                        }

                        setSession(currentSession);
                        IS_MARKIT_SESSION_CONNECTED = true;
                        CURRENT_CONNECTION_ERROR_COUNT.set(0);
                        MAX_RETRY_ALERT_SENT.set(false);
                    }

                } catch (ErrorCode err) {

                    // ✅ -301 cookie rejected => repartir de zéro avec une Session neuve
                    if (isCookieRejected(err)) {
                        LOGGER.warn("Cookie rejected (-301): rebuilding Session from scratch");

                        try { currentSession.disconnect(); } catch (Exception | ErrorCode ignore) {}

                        setSession(null);
                        IS_MARKIT_SESSION_CONNECTED = false;

                        currentSession = sessionFactory.get();
                    }

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
                requestApplicationShutdown(2); // exit code non-zéro
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

            if (MAX_RETRY_ALERT_SENT.compareAndSet(false, true)) {
                try {
                    Date currentDate = new Date();
                    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    if (jmsListner != null) {
                        jmsListner.send(
                                JMSListener.OutgoingType.FUNCTIONAL,
                                dateFormat.format(currentDate)
                                        + ": Le nombre de tentative de connexion a été atteint sur le connecteur markitdealer\n"
                                        + " le connecteur a été arrété."
                        );
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to send functional alert (ignored)", e);
                }
            }

            stopMarkitConnectorInternal();
            requestApplicationShutdown(2); // exit code non-zéro
        } else {
            LOGGER.info("Current connection retry count: " + attempt);
            LOGGER.info("Max connection retry count: " + maxConnectionRetryCount);
        }
    }

    /**
     * Arrêt propre Spring Boot (Tomcat, beans, etc.)
     * et sortie du process avec un code.
     */
    private void requestApplicationShutdown(int exitCode) {
        try {
            ConfigurableApplicationContext ctx = APP_CONTEXT;

            if (ctx == null) {
                LOGGER.error("ApplicationContext not set: fallback System.exit({})", exitCode);
                System.exit(exitCode);
                return;
            }

            // ✅ déclenche l’arrêt dans un thread séparé (évite d’embrouiller le worker)
            new Thread(() -> {
                try {
                    LOGGER.error("Max retry reached: shutting down Spring application (exitCode={})", exitCode);
                    int code = SpringApplication.exit(ctx, () -> exitCode);
                    System.exit(code);
                } catch (Throwable t) {
                    LOGGER.error("Shutdown failed: forcing System.exit({})", exitCode, t);
                    System.exit(exitCode);
                }
            }, "markit-shutdown").start();

        } catch (Throwable t) {
            LOGGER.error("requestApplicationShutdown failed: forcing System.exit({})", exitCode, t);
            System.exit(exitCode);
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

    // API conservée
    public void stopMarkitConnector() {
        if (INSTANCE != null) {
            INSTANCE.stopMarkitConnectorInternal();
        } else {
            DISCONNECTION_REQUEST_RECEIVED = true;
            IS_MARKIT_SESSION_CONNECTED = false;
            synchronized (EXECUTOR_LOCK) {
                if (executorService != null) {
                    executorService.shutdown();
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

    private void stopMarkitConnectorInternal() {
        DISCONNECTION_REQUEST_RECEIVED = true;
        IS_MARKIT_SESSION_CONNECTED = false;

        try {
            if (jmsListner != null) {
                jmsListner.stopListening();
            }
        } catch (Exception e) {
            LOGGER.error("stopListening failed (ignored)", e);
        }

        try {
            Session session = getSession();
            if (session != null) {
                try {
                    DealerAPIWrapper.logout(session.getLoginHandle());
                    LOGGER.info("Session logout");
                } catch (ErrorCode e) {
                    LOGGER.warn("logout failed (ignored): {}", safeMsg(e));
                } catch (Exception e) {
                    LOGGER.warn("logout failed (ignored): {}", safeMsg(e));
                }
            }
        } catch (Exception e) {
            LOGGER.error("stopMarkitConnectorInternal session cleanup failed (ignored)", e);
        } finally {
            setSession(null);
        }

        // ✅ ne pas shutdown/await depuis le worker thread
        if (Thread.currentThread() == MARKIT_WORKER_THREAD) {
            LOGGER.info("Stop requested from Markit worker thread: skipping executor shutdown/await.");
            return;
        }

        synchronized (EXECUTOR_LOCK) {
            if (executorService != null) {
                executorService.shutdown();
                try {
                    executorService.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                executorService = null;
            }
        }

        LOGGER.info("Markit connection stopped");
    }

    public synchronized void restartConnector() throws ErrorCode {
        try {
            try {
                if (IS_MARKIT_SESSION_CONNECTED) {
                    stopMarkitConnector();
                }
            } catch (Exception ignore) {
                // ignore
            }

            DISCONNECTION_REQUEST_RECEIVED = false;
            IS_MARKIT_SESSION_CONNECTED = false;
            CURRENT_CONNECTION_ERROR_COUNT.set(0);
            MAX_RETRY_ALERT_SENT.set(false);

            initializeMarkitConnector(true);

            LOGGER.info("Restart Markit connection in progress...");
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

    private static boolean isCookieRejected(ErrorCode err) {
        // ✅ ErrorCode expose "errorCode" (public final int)
        return err != null && err.errorCode == -301;
    }

    private static String safeMsg(Object e) {
        if (e == null) return "null";
        try {
            if (e instanceof Throwable) {
                String m = ((Throwable) e).getMessage();
                if (m != null && !m.trim().isEmpty()) return m;
            }
            return String.valueOf(e);
        } catch (Exception ex) {
            return String.valueOf(e);
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
