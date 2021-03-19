package clients;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class Consumer {

  static final String KAFKA_TOPIC  = (System.getenv("TOPIC") != null) ?
                                      System.getenv("TOPIC") : "default-topic";

  /**
   * Java consumer.
   */
  public static void main(String[] args) {
    System.out.println("Starting Java Consumer.");

    String clientId  = System.getenv("CLIENT_ID");
    clientId = (clientId != null) ? clientId : "consumer1";

    // Configure the group id, location of the bootstrap server, default deserializers, security
    final Properties settings = new Properties();
    settings.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
    settings.put(ConsumerConfig.GROUP_ID_CONFIG, "demo-consumer-group");
    settings.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "pkc-ep9mm.us-east-2.aws.confluent.cloud:9092");
    settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    settings.put("ssl.endpoint.identification.algorithm", "https");
    settings.put("security.protocol", "SASL_SSL");
    settings.put("sasl.mechanism", "PLAIN");
    settings.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"OWM5ZBX7QJNQ4A2N\" password=\"rtK3+rr1kRN0+O7z8OMT330nl0J1qf4yMGON1xOwp8Ttd/TuH1K3r7YP1yAi4LYj\";");

    final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(settings);

    try {
      // Subscribe to our topic
      consumer.subscribe(Arrays.asList(KAFKA_TOPIC));
      while (true) {
        // TODO: Poll for available records
        final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
          // TODO: print the contents of the record
          System.out.printf("Key:%s Value:%s [partition %s]\n",
              record.key(), record.value(), record.partition());
        }
      }
    } finally {
      // Clean up when the application exits or errors
      System.out.println("Closing consumer.");
      consumer.close();
    }
  }
}
