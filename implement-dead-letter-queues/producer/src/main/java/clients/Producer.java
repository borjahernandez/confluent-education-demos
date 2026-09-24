package clients;

import clients.UserProtos.UserOuterClass.User;
import demo.Config;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
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
 * The naive producer: parses each line of users-data-incr.csv into a Protobuf User and produces
 * it. Line 6 is malformed, so the first bad row throws and the whole producer dies.
 *
 * <p>Env vars: TOPIC (default "user-topic"), NUM_RECORDS (default 1,000,000), SLEEP_MS (default 1000).
 */
public class Producer {
  static final String KAFKA_TOPIC = Config.env("TOPIC", "user-topic");
  static final int NUM_RECORDS = Config.envInt("NUM_RECORDS", 1_000_000);
  static final int SLEEP_MS = Config.envInt("SLEEP_MS", 1000);

  public static void main(String[] args) throws Exception {
    final Properties settings = Config.load(args);
    settings.put(ProducerConfig.CLIENT_ID_CONFIG, "demo-producer");
    settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);

    // Format: Id|FirstName|LastName|Email|Birthday|RegistrationTimestamp|ActiveAccount
    final List<String> rows = Files.readAllLines(Path.of("../data/users-data-incr.csv"), StandardCharsets.UTF_8);

    try (KafkaProducer<String, User> producer = new KafkaProducer<>(settings)) {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> producer.close(Duration.ofSeconds(5))));

      for (int i = 0; i < NUM_RECORDS; i++) {
        final String line = rows.get(i % rows.size());
        final User user = Users.parse(line); // throws on a malformed line
        producer.send(new ProducerRecord<>(KAFKA_TOPIC, String.valueOf(user.getId()), user));
        System.out.println("Message sent: " + line);
        Thread.sleep(SLEEP_MS);
      }
    }
  }
}
