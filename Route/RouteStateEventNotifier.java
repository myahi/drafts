package eai.camel.core.lifecycle;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.event.EventNotifierSupport;
import org.apache.camel.management.event.RouteStartedEvent;
import org.apache.camel.management.event.RouteStoppedEvent;
import org.apache.camel.spi.CamelEvent;
import org.springframework.stereotype.Component;

@Component
public class RouteStateEventNotifier extends EventNotifierSupport {

  private final RouteStateRepository repository;

  public RouteStateEventNotifier(CamelContext camelContext,
                                 RouteStateRepository repository) {
    this.repository = repository;
    camelContext.getManagementStrategy().addEventNotifier(this);
  }

  @Override
  public void notify(CamelEvent event) {
    if (event instanceof RouteStoppedEvent e) {
      repository.upsert(e.getRoute().getId(), "STOPPED");
    }
    else if (event instanceof RouteStartedEvent e) {
      repository.upsert(e.getRoute().getId(), "STARTED");
    }
  }

  @Override
  public boolean isEnabled(CamelEvent event) {
    return event instanceof RouteStoppedEvent
        || event instanceof RouteStartedEvent;
  }
}
