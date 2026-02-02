package fr.labanquepostale.marches.eai.core.route.lifecycle;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppReadiness {

  private volatile boolean ready = false;

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    ready = true;
  }

  public boolean isReady() {
    return ready;
  }
}
