package clients;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConsumerGroup {
    static final String NUM_CONSUMERS  = (System.getenv("NUM_CONSUMERS") != null) ?
            System.getenv("NUM_CONSUMERS") : "1";
    static final String KAFKA_TOPIC  = (System.getenv("TOPIC") != null) ?
            System.getenv("TOPIC") : "default-topic";
    static final String AUTO_OFFSET_RESET  = (System.getenv("AUTO_OFFSET_RESET") != null) ?
            System.getenv("AUTO_OFFSET_RESET") : "earliest";

    public static void main(String[] args) throws IOException {
        int numConsumers = Integer.parseInt(NUM_CONSUMERS);
        String groupId = "consumer-group-demo-partitioner";
        List<String> topics = Arrays.asList(KAFKA_TOPIC);
        ExecutorService executor = Executors.newFixedThreadPool(numConsumers);

        final Properties settings = loadConfig(args[0]);
        final List<ConsumerLoop> consumers = new ArrayList<>();
        for (int i = 0; i < numConsumers; i++) {
            ConsumerLoop consumer = new ConsumerLoop(i, groupId, topics, AUTO_OFFSET_RESET, settings);
            consumers.add(consumer);
            executor.submit(consumer);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (ConsumerLoop consumer : consumers) {
                consumer.shutdown();
            }
            executor.shutdown();
            try {
                executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
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
