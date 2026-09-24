package clients;

import demo.Config;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Loops over data/inputData.csv (FirstName,LastName,Email,RegistrationDate,Country) and produces
 * each row as a String, keyed by Country.
 *
 * <p>Env vars: TOPIC (default "default-topic"), PARTITIONER ("default" or "custom"), BIG_KEY
 * (default "United States"), NUM_RECORDS (default 1,000,000), SLEEP_MS (default 0).
 */
public class Producer {
  static final String KAFKA_TOPIC = Config.env("TOPIC", "default-topic");
  static final int NUM_RECORDS = Config.envInt("NUM_RECORDS", 1_000_000);
  static final int SLEEP_MS = Config.envInt("SLEEP_MS", 0);

  public static void main(String[] args) throws Exception {
    final Properties settings = Config.load(args);
    settings.put(ProducerConfig.CLIENT_ID_CONFIG, "demo-partitioner-producer");
    settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    // PARTITIONER=custom routes the hot key to its own partition (see BigKeyPartitioner)
    if (Config.env("PARTITIONER", "default").equals("custom")) {
      settings.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, BigKeyPartitioner.class);
      settings.put(BigKeyPartitioner.BIG_KEY_CONFIG, Config.env("BIG_KEY", "United States"));
    }
    final List<String> rows = Files.readAllLines(Path.of("data/inputData.csv"), StandardCharsets.UTF_8);

    System.out.println("Producing " + NUM_RECORDS + " records to " + KAFKA_TOPIC);
    try (KafkaProducer<String, String> producer = new KafkaProducer<>(settings)) {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> producer.close(Duration.ofSeconds(5))));

      for (int i = 0; i < NUM_RECORDS; i++) {
        final String row = rows.get(i % rows.size());
        final String country = row.split(",")[4];
        producer.send(new ProducerRecord<>(KAFKA_TOPIC, country, row));
        if (SLEEP_MS > 0) {
          Thread.sleep(SLEEP_MS);
        }
      }
    }
    System.out.println("Done.");
  }
}
