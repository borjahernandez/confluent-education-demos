package clients;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import clients.UserProtos.UserOuterClass.User;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class Consumer {

  static final String KAFKA_TOPIC  = (System.getenv("TOPIC") != null) ?
                                      System.getenv("TOPIC") : "user-topic";

  /**
   * Java consumer.
   */
  public static void main(String[] args) throws IOException {
    System.out.println("Starting Java Consumer.");

    String clientId  = System.getenv("CLIENT_ID");
    clientId = (clientId != null) ? clientId : "consumer1-user-demo";

    // Creating the Kafka Consumer
    final Properties settings = loadConfig(args[0]);
    settings.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
    settings.put(ConsumerConfig.GROUP_ID_CONFIG, "demo-consumer-group");
    settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
    settings.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, User.class.getName());

    final KafkaConsumer<String, User> consumer = new KafkaConsumer<>(settings);

    //
    try {
      // Subscribe to our topic
      consumer.subscribe(Arrays.asList(KAFKA_TOPIC));
      while (true) {
        final ConsumerRecords<String, User> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, User> record : records) {
          System.out.println("\nKey: " + record.key()
                  + "\nMessage:"
                  + "\n\tID: " + record.value().getId()
                  + "\n\tFirst Name: " + record.value().getFirstName()
                  + "\n\tLast Name: " + record.value().getLastName()
                  + "\n\tEmail: " + record.value().getEmail()
                  + "\n\tBirthday: " + record.value().getBirthday()
                  + "\n\tRegistration Date: " + record.value().getRegTimestamp()
                  + "\n\tActive Account: " + record.value().getActiveAccount());
        }
      }
    } finally {
      // Clean up when the application exits or errors
      System.out.println("Closing consumer.");
      consumer.close();
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
