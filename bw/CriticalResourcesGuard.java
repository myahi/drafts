package fr.labanquepostale.marches.eai.core.ressources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.Phased;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class CriticalResourcesGuard implements Lifecycle,Phased{

	private final JdbcTemplate jdbc;
	private final Environment env;
	private final ApplicationContext ctx;
	
	private boolean isRunning = false;
	
	public CriticalResourcesGuard(JdbcTemplate jdbc, Environment env, ApplicationContext ctx) {
		this.jdbc = jdbc;
		this.env = env;
		this.ctx = ctx;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void checkOnStartup() {
		int retries = env.getProperty("resource.check.retry.count", Integer.class, 3);
		long delayMs = env.getProperty("resource.check.retry.delay-ms", Long.class, 5000L);

		for (int i = 1; i <= retries; i++) {
			try {
				checkAll();
				log.info("Startup resource check succeeded");
				isRunning = true;
				return;
			} catch (Exception e) {
				if (i == retries) {
					shutdown("Startup resource check failed", e);
				}
				sleep(delayMs);
			}
		}
	}

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
			var factory = new com.tibco.tibjms.TibjmsConnectionFactory(env.getRequiredProperty("tibco.ems.server-url"));
			try (var connection = factory.createConnection(env.getRequiredProperty("tibco.ems.username"), env.getRequiredProperty("tibco.ems.password"))) {
				connection.start();
			}
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
		} catch (Exception exception) {
		} finally {
			SpringApplication.exit(ctx, () -> 1);
			System.exit(1);
		}
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void start() {
		isRunning = true;
	}

	@Override
	public void stop() {
		isRunning = false;
	}

	@Override
	public int getPhase() {
		return Integer.MIN_VALUE;
	}
}
