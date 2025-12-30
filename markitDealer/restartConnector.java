public synchronized void restartConnector() {
    try {
        // stop propre si un thread tourne déjà
        try {
            stopMarkitConnector();
        } catch (Exception ignore) {
            // ignore
        }

        // reset état (important)
        DISCONNECTION_REQUEST_RECEIVED = false;
        IS_MARKIT_SESSION_CONNECTED = false;
        // si tu as AtomicInteger :
        // CURRENT_CONNECTION_ERROR_COUNT.set(0);
        current_connection_error_account = 0;

        // relance avec resetRetryCount=true
        initializeMarkitConnector(true);

        LOGGER.info("Markit connector restarted");
    } catch (Exception e) {
        LOGGER.error("restartConnector failed", e);
        throw new RuntimeException("restartConnector failed: " + e.getMessage(), e);
    }
}

