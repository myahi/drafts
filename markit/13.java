private void requestApplicationShutdown(int exitCode) {
    if (!SHUTDOWN_ONCE.compareAndSet(false, true)) return;

    ConfigurableApplicationContext ctx = APP_CONTEXT;

    // watchdog: si un shutdown hook bloque, on force l'arrêt
    Thread watchdog = new Thread(() -> {
        try {
            Thread.sleep(15000);
            LOGGER.error("Shutdown stuck -> forcing Runtime.halt({})", exitCode);
            Runtime.getRuntime().halt(exitCode);
        } catch (InterruptedException ignored) {}
    }, "shutdown-watchdog");
    watchdog.setDaemon(true);
    watchdog.start();

    try {
        if (ctx != null) {
            int code = SpringApplication.exit(ctx, () -> exitCode);
            System.exit(code);
        } else {
            System.exit(exitCode);
        }
    } catch (Throwable t) {
        LOGGER.error("Shutdown failed -> forcing Runtime.halt({})", exitCode, t);
        Runtime.getRuntime().halt(exitCode);
    }
}
