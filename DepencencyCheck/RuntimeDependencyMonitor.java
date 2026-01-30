package eai.camel.core.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RuntimeDependencyMonitor {

  private static final Logger log = LoggerFactory.getLogger(RuntimeDependencyMonitor.class);

  private final ConfigurableApplicationContext context;
  private final DependencyCheckProperties props;
  private final DependencyChecks checks;

  private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

  public RuntimeDependencyMonitor(ConfigurableApplicationContext context,
                                  DependencyCheckProperties props,
                                  DependencyChecks checks) {
    this.context = context;
    this.props = props;
    this.checks = checks;
  }

  @Scheduled(fixedDelayString = "#{@dependencyCheckProperties.runtime.periodMs}")
  public void monitor() {
    if (!props.isEnabled() || !props.getRuntime().isEnabled()) return;

    var result = checks.checkAll();

    if (result.ok()) {
      int prev = consecutiveFailures.getAndSet(0);
      if (prev > 0) {
        log.info("Runtime dependency checks recovered (BDD/EMS/FS).");
      }
      return;
    }

    int fails = consecutiveFailures.incrementAndGet();
    int maxFails = Math.max(1, props.getRuntime().getMaxConsecutiveFailures());

    log.warn("Runtime dependency checks failed (consecutive {}/{}): {}", fails, maxFails, result.message());

    if (fails >= maxFails) {
      log.error("Runtime dependency checks failing {} times in a row. Stopping application.", fails);
      stopApp(2);
    }
  }

  private void stopApp(int code) {
    int exit = SpringApplication.exit(context, () -> code);
    System.exit(exit);
  }
}
