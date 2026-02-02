package fr.labanquepostale.marches.eai.core.route.lifecycle;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.CamelEvent;

import org.apache.camel.impl.event.RouteStartedEvent;
import org.apache.camel.impl.event.RouteStoppedEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RouteStateEventNotifier extends EventNotifierSupport {

	private final RouteStateRepository repository;

	private final AppReadiness readiness;
	private final Environment env;
	private String instanceName;

	public RouteStateEventNotifier(CamelContext camelContext, RouteStateRepository repository, AppReadiness readiness, Environment env) {
		this.env = env;
		this.repository = repository;
		this.instanceName = this.env.getProperty("app.instance-name");
		this.readiness = readiness;
		camelContext.getManagementStrategy().addEventNotifier(this);
	}

	@Override
	public void notify(CamelEvent event) {
		if (!readiness.isReady()) {
			return;
		}

		if (event instanceof RouteStoppedEvent e) {
			repository.upsert(instanceName, e.getRoute().getId(), "STOPPED");
		} else if (event instanceof RouteStartedEvent e) {
			repository.upsert(instanceName, e.getRoute().getId(), "STARTED");
		}
	}

	@Override
	public boolean isEnabled(CamelEvent event) {
		return event instanceof RouteStoppedEvent || event instanceof RouteStartedEvent;
	}

}
