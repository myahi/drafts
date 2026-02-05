package fr.labanquepostale.fix.connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import fr.labanquepostale.fix.connection.config.ApplicationProperties;
import fr.labanquepostale.tools.FixLogFactory;
import quickfix.DefaultMessageFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

@SpringBootApplication
@ComponentScan(basePackages = "fr.labanquepostale")
public class FixClientStarter {
	private static final Logger LOGGER = LogManager.getLogger(FixClientStarter.class);
	static SocketInitiator SOCKET_INITIATOR = null;

	public static void main(String[] args) {
		final ConfigurableApplicationContext ctx = SpringApplication.run(FixClientStarter.class, args);

		try {
			// args[0] = chemin settings FIX
			if (args.length < 1) {
				throw new IllegalArgumentException("Usage: <fixSettingsFile>");
			}
			ApplicationProperties props = ctx.getBean(ApplicationProperties.class);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				LOGGER.info("Fix engine will be shutdown.");
				try {
					if (SOCKET_INITIATOR != null) {
						SOCKET_INITIATOR.stop();
					}
					if (ctx != null) {
						ctx.close();
					}
				} catch (Exception e) {
					LOGGER.error("Error while stopping fix application", e);
				}
			}));
			SessionSettings sessionSettings = new SessionSettings(args[0]);
			FixApplication application = new FixApplication(props);
			MessageFactory messageFactory = new DefaultMessageFactory();
			MessageStoreFactory fileStoreFactory = new FileStoreFactory(sessionSettings);
			LogFactory logFactory = new FixLogFactory();
			SOCKET_INITIATOR = new SocketInitiator(application, fileStoreFactory, sessionSettings, logFactory, messageFactory);
			SOCKET_INITIATOR.start();
			while (true) {
				Thread.sleep(5000);
			}

		} catch (Exception exp) {
			LOGGER.error(exp);
		}
	}
}
