package fr.labanquepostale.marches.eai.core.route.lifecycle;

import org.apache.camel.CamelContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RouteStartupManager {

	
	private final CamelContext camelContext;
	private final RouteStateRepository repository;
	private final String instanceName;
	private final Environment env;
	
	public RouteStartupManager(CamelContext camelContext, RouteStateRepository repository,Environment env) {
		this.camelContext = camelContext;
		this.env = env;
		this.instanceName = this.env.getProperty("app.instance-name");
		this.repository = repository;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void startRoutes() throws Exception {

		camelContext.start();

		var states = repository.loadAllForInstance(instanceName);

		for (var route : camelContext.getRoutes()) {
			String routeId = route.getId();
			String desired = states.getOrDefault(routeId, "STARTED");

			if ("STOPPED".equalsIgnoreCase(desired)) {
				continue;
			}

			camelContext.getRouteController().startRoute(routeId);
		}
	}
}
