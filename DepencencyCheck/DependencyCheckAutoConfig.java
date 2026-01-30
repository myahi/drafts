package eai.camel.core.health;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for dependency checks (startup + runtime).
 *
 * - Active le binding des propriétés eai.dependency-check.*
 * - Active le scheduler Spring pour les checks runtime
 *
 * Cette classe ne contient aucune logique métier.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(DependencyCheckProperties.class)
public class DependencyCheckAutoConfig {
  // Intentionnellement vide
}
