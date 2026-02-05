package fr.labanquepostale.marches.eai.core.route.lifecycle;

import org.apache.camel.CamelContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RouteStartupManager {

    private final CamelContext camelContext;
    private final RouteStateRepository repository;
    private final String instanceName;
    private final AppReadiness readiness;

    public RouteStartupManager(CamelContext camelContext,RouteStateRepository repository,Environment env,AppReadiness readiness) {
        this.camelContext = camelContext;
        this.repository = repository;
        this.instanceName = env.getRequiredProperty("app.instance-name");
        this.readiness = readiness;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startRoutes() throws Exception {

        // 1) Start CamelContext (routes are still NOT started thanks to camel.main.auto-startup=false)
        camelContext.start();
        log.info("CamelContext status ={}",camelContext.getStatus());
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
            	log.info("routeId: {}, status: {}", routeId, camelContext.getRouteController().getRouteStatus(routeId));
                continue;
            }
            camelContext.getRouteController().startRoute(routeId);
            log.info("routeId: {}, status: {}", routeId, camelContext.getRouteController().getRouteStatus(routeId));
        }

        // 4) Only now: allow EventNotifier to persist Hawtio changes
        readiness.markReady();
    }
}
