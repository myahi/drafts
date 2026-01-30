package eai.camel.core.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eai.dependency-check")
public class DependencyCheckProperties {

  private boolean enabled = true;
  private Startup startup = new Startup();
  private Runtime runtime = new Runtime();
  private Filesystem filesystem = new Filesystem();

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public Startup getStartup() { return startup; }
  public void setStartup(Startup startup) { this.startup = startup; }

  public Runtime getRuntime() { return runtime; }
  public void setRuntime(Runtime runtime) { this.runtime = runtime; }

  public Filesystem getFilesystem() { return filesystem; }
  public void setFilesystem(Filesystem filesystem) { this.filesystem = filesystem; }

  public static class Startup {
    private int maxAttempts = 30;
    private long delayMs = 2000;
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public long getDelayMs() { return delayMs; }
    public void setDelayMs(long delayMs) { this.delayMs = delayMs; }
  }

  public static class Runtime {
    private boolean enabled = true;
    private long periodMs = 30000;
    private int maxConsecutiveFailures = 3;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getPeriodMs() { return periodMs; }
    public void setPeriodMs(long periodMs) { this.periodMs = periodMs; }
    public int getMaxConsecutiveFailures() { return maxConsecutiveFailures; }
    public void setMaxConsecutiveFailures(int maxConsecutiveFailures) { this.maxConsecutiveFailures = maxConsecutiveFailures; }
  }

  public static class Filesystem {
    private String path = "/tmp";
    private boolean requireWritable = true;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public boolean isRequireWritable() { return requireWritable; }
    public void setRequireWritable(boolean requireWritable) { this.requireWritable = requireWritable; }
  }
}
