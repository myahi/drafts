package eai.camel.core.lifecycle;

import org.apache.camel.CamelContext;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
public class RouteStateStartupApplier {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  ApplicationRunner applyRouteStates(CamelContext camelContext,
                                     RouteStateRepository repository) {

    return args -> {
      var states = repository.loadAll();

      for (var entry : states.entrySet()) {
        String routeId = entry.getKey();
        String desiredState = entry.getValue();

        // route supprimée / renommée → ignore
        if (camelContext.getRoute(routeId) == null) {
          continue;
        }

        if ("STOPPED".equalsIgnoreCase(desiredState)) {
          camelContext
            .getRouteController()
            .stopRoute(routeId);
        }
      }
    };
  }
}
