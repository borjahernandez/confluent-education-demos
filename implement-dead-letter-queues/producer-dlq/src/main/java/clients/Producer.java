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
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * The same producer with a dead letter queue: a line that fails to parse is sent, untouched, to
 * the DLQ topic with the error in the record headers, and the producer carries on.
 *
 * <p>Env vars: TOPIC (default "user-topic"), DLQ_TOPIC (default "user-topic-dlq"),
 * NUM_RECORDS (default 1,000,000), SLEEP_MS (default 1000).
 */
public class Producer {
  static final String KAFKA_TOPIC = Config.env("TOPIC", "user-topic");
  static final String DLQ_TOPIC = Config.env("DLQ_TOPIC", "user-topic-dlq");
  static final int NUM_RECORDS = Config.envInt("NUM_RECORDS", 1_000_000);
  static final int SLEEP_MS = Config.envInt("SLEEP_MS", 1000);

  public static void main(String[] args) throws Exception {
    final Properties settings = Config.load(args);
    settings.put(ProducerConfig.CLIENT_ID_CONFIG, "demo-producer");
    settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);

    // A second producer for the invalid messages: plain Strings, since they are not valid Users
    final Properties dlqSettings = Config.load(args);
    dlqSettings.put(ProducerConfig.CLIENT_ID_CONFIG, "demo-dlq-producer");
    dlqSettings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    dlqSettings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    // Format: Id|FirstName|LastName|Email|Birthday|RegistrationTimestamp|ActiveAccount
    final List<String> rows = Files.readAllLines(Path.of("../data/users-data-incr.csv"), StandardCharsets.UTF_8);

    try (KafkaProducer<String, User> producer = new KafkaProducer<>(settings);
         KafkaProducer<String, String> dlqProducer = new KafkaProducer<>(dlqSettings)) {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        producer.close(Duration.ofSeconds(5));
        dlqProducer.close(Duration.ofSeconds(5));
      }));

      for (int i = 0; i < NUM_RECORDS; i++) {
        final String line = rows.get(i % rows.size());
        final User user;
        try {
          user = Users.parse(line);
        } catch (RuntimeException e) {
          // Keep the original line as the value and put the diagnosis in headers, so the DLQ can be
          // inspected, fixed and replayed without parsing an error message back out of the value
          final RecordHeaders headers = new RecordHeaders();
          headers.add("error.class", e.getClass().getName().getBytes(StandardCharsets.UTF_8));
          headers.add("error.message", String.valueOf(e.getMessage()).getBytes(StandardCharsets.UTF_8));
          headers.add("source.line", String.valueOf(i % rows.size() + 1).getBytes(StandardCharsets.UTF_8));
          dlqProducer.send(new ProducerRecord<>(DLQ_TOPIC, null, null, line, headers));
          System.out.println("INVALID MESSAGE sent to " + DLQ_TOPIC + ": " + line + "  (" + e + ")");
          continue;
        }
        producer.send(new ProducerRecord<>(KAFKA_TOPIC, String.valueOf(user.getId()), user));
        System.out.println("Message sent: " + line);
        Thread.sleep(SLEEP_MS);
      }
    }
  }
}
