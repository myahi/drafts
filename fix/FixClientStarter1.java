package fr.labanquepostale.fix.connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import fr.labanquepostale.fix.connection.config.ApplicationProperties;
import fr.labanquepostale.tools.FixLogFactory;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

@SpringBootApplication
public class FixClientStarter {
  private static final Logger LOGGER = LogManager.getLogger(FixClientStarter.class);
  static SocketInitiator SOCKET_INITIATOR = null;

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = null;
    try {
      // args[0] = chemin settings FIX
      // args[1] = incomingMsgFolderPath
      if (args.length < 2) {
        throw new IllegalArgumentException("Usage: <fixSettingsFile> <incomingMsgFolderPath>");
      }

      ctx = SpringApplication.run(FixClientStarter.class, args);

      ApplicationProperties props = ctx.getBean(ApplicationProperties.class);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        LOGGER.info("Fix engine will be shutdown.");
        try {
          if (SOCKET_INITIATOR != null) SOCKET_INITIATOR.stop();
        } catch (Exception e) {
          LOGGER.error("Error while stopping initiator", e);
        }
        try {
          if (ctx != null) ctx.close();
        } catch (Exception e) {
          LOGGER.error("Error while closing Spring context", e);
        }
      }));

      SessionSettings sessionSettings = new SessionSettings(args[0]);

      FixApplication application = new FixApplication(args[1], props);

      MessageFactory messageFactory = new DefaultMessageFactory();
      MessageStoreFactory fileStoreFactory = new FileStoreFactory(sessionSettings);
      LogFactory logFactory = new FixLogFactory();

      SOCKET_INITIATOR = new SocketInitiator(application, fileStoreFactory, sessionSettings, logFactory, messageFactory);
      SOCKET_INITIATOR.start();

      while (true) {
        Thread.sleep(5000);
      }

    } catch (ConfigError e) {
      LOGGER.error(e);
    } catch (Exception exp) {
      LOGGER.error(exp);
    }
  }
}
