package demo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Helpers shared by every demo client: connection settings from a file, knobs from env vars. */
public final class Config {
  private Config() {}

  /** Loads the client properties file passed as the first program argument. */
  public static Properties load(String[] args) throws IOException {
    if (args.length == 0) {
      throw new IllegalArgumentException("Usage: <client properties file>, e.g. config/local.properties");
    }
    final Path path = Path.of(args[0]);
    if (!Files.exists(path)) {
      throw new FileNotFoundException(path + " not found. Use config/local.properties, or copy "
          + "config/ccloud.properties.template to config/ccloud.properties and fill it in.");
    }
    final Properties settings = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      settings.load(in);
    }
    return settings;
  }

  public static String env(String name, String defaultValue) {
    final String value = System.getenv(name);
    return (value == null || value.isBlank()) ? defaultValue : value;
  }

  public static int envInt(String name, int defaultValue) {
    return Integer.parseInt(env(name, String.valueOf(defaultValue)));
  }
}
