package fr.labanquepostale.marches.eai.core.ressources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.Phased;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tibco.tibjms.TibjmsConnectionFactory;

import jakarta.jms.Connection;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class CriticalResourcesGuard implements Lifecycle, Phased {

	private final JdbcTemplate jdbc;
	private final Environment env;
	private final ApplicationContext ctx;

	@Value("${tibco.ems.server-url}")
	private String serverUrl;

	@Value("${tibco.ems.username}")
	private String username;

	@Value("${tibco.ems.password}")
	private String password;

	@Value("${tibco.ems.pool.max-connections:10}")
	private int maxConnections;

	@Value("${tibco.ems.reconnect.attempts:3}")
	private int reconnectAttempts;

	@Value("${tibco.ems.reconnect.delay:5000}")
	private int reconnectDelay;

	// visible across threads
	private volatile boolean running = false;

	public CriticalResourcesGuard(JdbcTemplate jdbc, Environment env, ApplicationContext ctx) {
		this.jdbc = jdbc;
		this.env = env;
		this.ctx = ctx;
	}

	/**
	 * Spring will call this during context startup. Because we implement Phased and
	 * return MIN_VALUE, this starts first.
	 *
	 * IMPORTANT: only set running=true AFTER checks succeed.
	 */
	@Override
	public void start() {
		int retries = env.getProperty("resource.check.retry.count", Integer.class, 3);
		long delayMs = env.getProperty("resource.check.retry.delay-ms", Long.class, 5000L);

		for (int i = 1; i <= retries; i++) {
			try {
				checkAll();
				log.info("Startup resource check succeeded");
				running = true;
				return;
			} catch (Exception e) {
				if (i == retries) {
					shutdown("Startup resource check failed", e);
					return; // for completeness (shutdown exits)
				}
				sleep(delayMs);
			}
		}
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	/**
	 * Lower value = started earlier. MIN_VALUE means: start this bean as early as
	 * possible.
	 */
	@Override
	public int getPhase() {
		return Integer.MIN_VALUE;
	}

	/**
	 * Runtime periodic check: If critical resources fail at runtime, shutdown.
	 *
	 * NOTE: This job will only start running after the scheduler is active. That's
	 * fine: it doesn't participate in ordering.
	 */
	@Scheduled(fixedDelayString = "${resource.check.period-ms}")
	public void periodicCheck() {
		int retries = env.getProperty("resource.check.retry.count", Integer.class, 3);
		long delayMs = env.getProperty("resource.check.retry.delay-ms", Long.class, 5000L);

		for (int i = 1; i <= retries; i++) {
			try {
				checkAll();
				return;
			} catch (Exception e) {
				if (i == retries) {
					shutdown("Runtime resource failure", e);
				}
				sleep(delayMs);
			}
		}
	}

	private void checkAll() {
		checkOracle();
		checkEms();
		checkFilesystem();
	}

	private void checkOracle() {
		jdbc.queryForObject("select 1 from dual", Integer.class);
	}

	private void checkEms() {
		try {
			TibjmsConnectionFactory factory = new TibjmsConnectionFactory();
			factory.setServerUrl(serverUrl);
			factory.setUserName(username);
			factory.setUserPassword(password);
			Connection connection = factory.createConnection();
			connection.start();
			
		} catch (Exception e) {
			throw new IllegalStateException("EMS unavailable", e);
		}
	}

	private void checkFilesystem() {
		Path path = Paths.get(env.getRequiredProperty("resource.fs.path"));
		if (!Files.isDirectory(path) || !Files.isReadable(path)) {
			throw new IllegalStateException("Filesystem not accessible: " + path);
		}
	}

	private void shutdown(String reason, Exception e) {
		try {
			log.error(reason, e);
		} catch (Exception ignored) {
		} finally {
			SpringApplication.exit(ctx, () -> 1);
		}
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}
}
