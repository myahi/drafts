package fr.labanquepostale.fix.connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
		try {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					LOGGER.info("Fix engine will be shutdown.");
					SOCKET_INITIATOR.stop();
				}
			});
			SessionSettings sessionSettings = new SessionSettings(args[0]);
			//FixApplication application = new FixApplication(args[1], args[2]);
			FixApplication application = new FixApplication(args[1], null);
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
