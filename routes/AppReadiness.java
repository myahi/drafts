package fr.labanquepostale.marches.eai.core.route.lifecycle;

import org.springframework.stereotype.Component;

@Component
public class AppReadiness {

  private volatile boolean ready = false;

//  @EventListener(ApplicationReadyEvent.class)
//  public void onReady() {
//    ready = true;
//  }
  
  public void markReady() {
	    ready = true;
	  }

  public boolean isReady() {
    return ready;
  }
}
