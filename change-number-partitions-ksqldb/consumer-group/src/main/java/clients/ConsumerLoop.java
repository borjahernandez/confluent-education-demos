package clients;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

/** One consumer of the group, printing which partition and offset every record came from. */
public class ConsumerLoop implements Runnable {
  private final KafkaConsumer<String, String> consumer;
  private final String topic;
  private final int id;

  public ConsumerLoop(int id, String groupId, String topic, String autoOffsetReset, Properties baseSettings) {
    this.id = id;
    this.topic = topic;
    // Copy, so the consumers never share (and overwrite) one Properties object
    final Properties settings = new Properties();
    settings.putAll(baseSettings);
    settings.put(ConsumerConfig.CLIENT_ID_CONFIG, "consumer-" + id);
    settings.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
    settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    this.consumer = new KafkaConsumer<>(settings);
  }

  @Override
  public void run() {
    try {
      consumer.subscribe(List.of(topic));
      while (true) {
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(100))) {
          System.out.printf("Consumer-%d: {partition=%d, offset=%d, key=%s, value=%s}%n",
              id, record.partition(), record.offset(), record.key(), record.value());
        }
      }
    } catch (WakeupException e) {
      // expected on shutdown
    } finally {
      consumer.close();
    }
  }

  public void shutdown() {
    consumer.wakeup();
  }
}
