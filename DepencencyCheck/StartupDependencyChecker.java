package eai.camel.core.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartupDependencyChecker implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(StartupDependencyChecker.class);

  private final ConfigurableApplicationContext context;
  private final DependencyCheckProperties props;
  private final DependencyChecks checks;

  public StartupDependencyChecker(ConfigurableApplicationContext context,
                                  DependencyCheckProperties props,
                                  DependencyChecks checks) {
    this.context = context;
    this.props = props;
    this.checks = checks;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (!props.isEnabled()) {
      log.info("Dependency checks disabled (eai.dependency-check.enabled=false).");
      return;
    }

    int max = Math.max(1, props.getStartup().getMaxAttempts());
    long delay = Math.max(0, props.getStartup().getDelayMs());

    for (int attempt = 1; attempt <= max; attempt++) {
      var result = checks.checkAll();

      if (result.ok()) {
        log.info("Startup dependency checks OK (BDD/EMS/FS).");
        return;
      }

      log.warn("Startup dependency checks failed (attempt {}/{}): {}", attempt, max, result.message());

      if (attempt < max && delay > 0) {
        Thread.sleep(delay);
      }
    }

    log.error("Startup dependency checks still failing after {} attempts. Stopping application.", max);
    stopApp(2);
  }

  private void stopApp(int code) {
    int exit = SpringApplication.exit(context, () -> code);
    System.exit(exit);
  }
}
