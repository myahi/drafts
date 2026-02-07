private void requestApplicationShutdown(int exitCode) {
    if (!SHUTDOWN_ONCE.compareAndSet(false, true)) return;

    final ConfigurableApplicationContext ctx = APP_CONTEXT;
    if (ctx == null) {
        System.err.println("APP_CONTEXT null -> forcing halt(" + exitCode + ")");
        Runtime.getRuntime().halt(exitCode);
        return;
    }

    Thread watchdog = new Thread(() -> {
        try {
            Thread.sleep(8000); // 8s
            System.err.println("Shutdown stuck -> forcing halt(" + exitCode + ")");
            Runtime.getRuntime().halt(exitCode);
        } catch (InterruptedException ignored) {}
    }, "shutdown-watchdog");
    watchdog.setDaemon(true);
    watchdog.start();

    new Thread(() -> {
        try {
            System.err.println("Shutting down Spring (exitCode=" + exitCode + ")");
            int code = SpringApplication.exit(ctx, () -> exitCode);
            System.exit(code);
        } catch (Throwable t) {
            System.err.println("Shutdown failed -> forcing halt(" + exitCode + "): " + t);
            Runtime.getRuntime().halt(exitCode);
        }
    }, "markit-shutdown").start();
}
