package clients;

import clients.UserProtos.UserOuterClass.User;
import demo.Config;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * The consumer side of the pattern. A record that cannot be deserialized into a User (a "poison
 * pill") would normally make poll() throw on every call, forever. Instead we copy its raw bytes to
 * the DLQ topic, seek past it and keep consuming.
 *
 * <p>Env vars: TOPIC (default "user-topic"), DLQ_TOPIC (default "user-topic-dlq").
 */
public class Consumer {
  static final String KAFKA_TOPIC = Config.env("TOPIC", "user-topic");
  static final String DLQ_TOPIC = Config.env("DLQ_TOPIC", "user-topic-dlq");

  public static void main(String[] args) throws Exception {
    final Properties settings = Config.load(args);
    settings.put(ConsumerConfig.CLIENT_ID_CONFIG, Config.env("CLIENT_ID", "consumer-user-demo"));
    settings.put(ConsumerConfig.GROUP_ID_CONFIG, "demo-consumer-group");
    settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
    settings.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, User.class.getName());

    final Properties dlqSettings = Config.load(args);
    dlqSettings.put(ProducerConfig.CLIENT_ID_CONFIG, "demo-consumer-dlq-producer");
    dlqSettings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    dlqSettings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

    final KafkaConsumer<String, User> consumer = new KafkaConsumer<>(settings);
    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      consumer.wakeup();
      try {
        mainThread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }));

    try (consumer; KafkaProducer<byte[], byte[]> dlqProducer = new KafkaProducer<>(dlqSettings)) {
      consumer.subscribe(List.of(KAFKA_TOPIC));
      while (true) {
        try {
          for (ConsumerRecord<String, User> record : consumer.poll(Duration.ofMillis(100))) {
            final User user = record.value();
            System.out.printf("%nKey: %s [partition %d, offset %d]%n\tID: %d%n\tName: %s %s%n\tEmail: %s"
                    + "%n\tBirthday: %s%n\tRegistered: %d%n\tActive: %b%n",
                record.key(), record.partition(), record.offset(), user.getId(), user.getFirstName(),
                user.getLastName(), user.getEmail(), user.getBirthday(), user.getRegTimestamp(),
                user.getActiveAccount());
          }
        } catch (RecordDeserializationException e) {
          // The exception carries the raw bytes and the exact position of the bad record
          final RecordHeaders headers = new RecordHeaders();
          headers.add("error.class", e.getCause().getClass().getName().getBytes(StandardCharsets.UTF_8));
          headers.add("error.message", String.valueOf(e.getCause().getMessage()).getBytes(StandardCharsets.UTF_8));
          headers.add("source.topic", e.topicPartition().topic().getBytes(StandardCharsets.UTF_8));
          headers.add("source.partition", String.valueOf(e.topicPartition().partition()).getBytes(StandardCharsets.UTF_8));
          headers.add("source.offset", String.valueOf(e.offset()).getBytes(StandardCharsets.UTF_8));
          dlqProducer.send(new ProducerRecord<>(DLQ_TOPIC, null, null, bytes(e.keyBuffer()), bytes(e.valueBuffer()), headers)).get();
          System.out.printf("%nPOISON PILL at %s offset %d sent to %s: %s%n",
              e.topicPartition(), e.offset(), DLQ_TOPIC, e.getCause().getMessage());
          consumer.seek(e.topicPartition(), e.offset() + 1);
        }
      }
    } catch (WakeupException e) {
      // expected on shutdown
    }
  }

  private static byte[] bytes(ByteBuffer buffer) {
    if (buffer == null) {
      return null;
    }
    final byte[] out = new byte[buffer.remaining()];
    buffer.duplicate().get(out);
    return out;
  }
}
