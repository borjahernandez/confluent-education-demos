package clients;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class Producer {
  static final String DATA_FILE_PREFIX = "./data/";
  static final String KAFKA_TOPIC  = (System.getenv("TOPIC") != null) ?
                                      System.getenv("TOPIC") : "five-partitions-topic";

  /**
   * Java producer.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("Starting Java producer.");

    // Configure the location of the bootstrap server, default serializers, security
    final Properties settings = loadConfig(args[0]);
    settings.put(ProducerConfig.CLIENT_ID_CONFIG, "demo-producer");
    settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    settings.put("ssl.endpoint.identification.algorithm", "https");
    settings.put("security.protocol", "SASL_SSL");
    settings.put("sasl.mechanism", "PLAIN");

    final KafkaProducer<String, String> producer = new KafkaProducer<>(settings);
    
    // Adding a shutdown hook to clean up when the application exits
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Closing producer.");
      producer.close();
    }));

    int pos = 0;
    // Format of inputData.csv (100 records) --> FirstName,LastName,Email,RegistrationDate,Country
    final String[] rows = Files.readAllLines(Paths.get(DATA_FILE_PREFIX + "inputData.csv"),
            StandardCharsets.UTF_8).toArray(new String[0]);

    // Loop forever over the driver CSV file..
    // Using key = "Country"
    String numRecordsString  = System.getenv("NUMBER_RECORDS");
    numRecordsString = (numRecordsString != null) ? numRecordsString : "1000000";
    int numRecords = Integer.parseInt(numRecordsString);

    for (int i = 0; i < numRecords; i++) {
      final String key = rows[pos].split(",")[4];
      final String value = rows[pos];

      final ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_TOPIC, key, value);
      producer.send(record);
      Thread.sleep(1);
      pos = (pos + 1) % rows.length;
    }
  }

  public static Properties loadConfig(final String configFile) throws IOException {
    if (!Files.exists(Paths.get(configFile))) {
      throw new IOException(configFile + " not found.");
    }
    final Properties cfg = new Properties();
    try (InputStream inputStream = new FileInputStream(configFile)) {
      cfg.load(inputStream);
    }
    return cfg;
  }
}