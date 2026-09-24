package clients;

import demo.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Runs NUM_CONSUMERS consumers in one consumer group, one thread each, so you can watch how the
 * partitions of a topic are shared out between them.
 *
 * <p>Env vars: TOPIC (default "default-topic"), NUM_CONSUMERS (default 1), AUTO_OFFSET_RESET (default "earliest").
 */
public class ConsumerGroup {
  static final String KAFKA_TOPIC = Config.env("TOPIC", "default-topic");
  static final int NUM_CONSUMERS = Config.envInt("NUM_CONSUMERS", 1);
  static final String AUTO_OFFSET_RESET = Config.env("AUTO_OFFSET_RESET", "earliest");
  static final String GROUP_ID = "consumer-group-demo-partitioner";

  public static void main(String[] args) throws Exception {
    final Properties settings = Config.load(args);
    final ExecutorService executor = Executors.newFixedThreadPool(NUM_CONSUMERS);
    final List<ConsumerLoop> consumers = new ArrayList<>();
    for (int i = 0; i < NUM_CONSUMERS; i++) {
      final ConsumerLoop consumer = new ConsumerLoop(i, GROUP_ID, KAFKA_TOPIC, AUTO_OFFSET_RESET, settings);
      consumers.add(consumer);
      executor.submit(consumer);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      consumers.forEach(ConsumerLoop::shutdown);
      executor.shutdown();
      try {
        executor.awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }));
  }
}
