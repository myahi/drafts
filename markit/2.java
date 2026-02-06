private void requestApplicationShutdown(int exitCode) {
    if (!SHUTDOWN_ONCE.compareAndSet(false, true)) {
        return;
    }

    ConfigurableApplicationContext ctx = APP_CONTEXT;
    if (ctx == null) {
        LOGGER.error("ApplicationContext not set: fallback System.exit({})", exitCode);
        System.exit(exitCode);
        return;
    }

    // Watchdog: si un shutdown hook bloque, on force le kill JVM
    Thread watchdog = new Thread(() -> {
        try {
            Thread.sleep(15000); // 15s (mets en conf si tu veux)
            LOGGER.error("Shutdown watchdog triggered -> forcing Runtime.halt({})", exitCode);
            Runtime.getRuntime().halt(exitCode);
        } catch (InterruptedException ignored) {
        }
    }, "shutdown-watchdog");
    watchdog.setDaemon(true);
    watchdog.start();

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
}
