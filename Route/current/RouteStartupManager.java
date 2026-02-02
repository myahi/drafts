@Component
public class RouteStartupManager {

    private final CamelContext camelContext;
    private final RouteStateRepository repository;
    private final String instanceName;
    private final AppReadiness readiness;

    public RouteStartupManager(CamelContext camelContext,
                               RouteStateRepository repository,
                               Environment env,
                               AppReadiness readiness) {
        this.camelContext = camelContext;
        this.repository = repository;
        this.instanceName = env.getRequiredProperty("app.instance-name");
        this.readiness = readiness;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startRoutes() throws Exception {

        // 1) Start CamelContext (routes are still NOT started thanks to camel.main.auto-startup=false)
        camelContext.start();

        // 2) Load desired states from DB
        var states = repository.loadAllForInstance(instanceName);

        // 3) Apply state route by route
        for (var route : camelContext.getRoutes()) {
            String routeId = route.getId();

            // New route => insert STARTED by default
            String desired = states.get(routeId);
            if (desired == null) {
                desired = "STARTED";
                repository.upsert(instanceName, routeId, "STARTED");
            }

            if ("STOPPED".equalsIgnoreCase(desired)) {
                // Ensure stopped (optional safety)
                // camelContext.getRouteController().stopRoute(routeId);
                continue;
            }

            camelContext.getRouteController().startRoute(routeId);
        }

        // 4) Only now: allow EventNotifier to persist Hawtio changes
        readiness.markReady();
    }
}
