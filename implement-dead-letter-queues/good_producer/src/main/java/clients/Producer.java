package clients;

import clients.UserProtos.UserOuterClass.User;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
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
  static final String KAFKA_TOPIC = (System.getenv("TOPIC") != null) ? System.getenv("TOPIC") : "user-topic";
  static final int NUM_RECORDS = Integer.parseInt((System.getenv("NUM_RECORDS") != null) ? System.getenv("NUM_RECORDS") : "1000000");

  /**
   * Java producer.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("Starting Java producer.");

    // Creating the Kafka producer
    final Properties settings = loadConfig(args[0]);
    settings.put(ProducerConfig.CLIENT_ID_CONFIG, "demo-producer");
    settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);

    final KafkaProducer<String, User> producer = new KafkaProducer<>(settings);

    
    // Adding a shutdown hook to clean up when the application exits
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Closing producers.");
      producer.close();
    }));

    int pos = 0;
    // Format of users-data-incr.csv (1000 records) --> Id,FirstName,LastName,Email,Birthday,RegistrationTimestamp,ActiveAccount
    final String[] rows = Files.readAllLines(Paths.get(DATA_FILE_PREFIX + "users-data-incr.csv"),
            StandardCharsets.UTF_8).toArray(new String[0]);

    for (int i = 0; i < NUM_RECORDS; i++) {
      final String line = rows[pos];
      final String[] values = line.split("\\|");
      final User message = User.newBuilder()
              .setId(Integer.parseInt(values[0]))
              .setFirstName(values[1])
              .setLastName(values[2])
              .setEmail(values[3])
              .setBirthday(values[4])
              .setRegTimestamp(Long.parseLong(values[5]))
              .setActiveAccount(Boolean.parseBoolean(values[6]))
              .build();
      final String key = values[0];

      final ProducerRecord<String, User> record = new ProducerRecord<>(KAFKA_TOPIC, key, message);
      producer.send(record);
      System.out.println("Message sent: " + line);
      Thread.sleep(1000);
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