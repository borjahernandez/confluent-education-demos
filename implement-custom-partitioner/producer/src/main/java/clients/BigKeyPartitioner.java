package clients;

import java.util.Map;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.InvalidRecordException;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.utils.Utils;

/**
 * Sends the "hot" key to partition 0 on its own and spreads every other key over the remaining
 * partitions with the same murmur2 hash Kafka uses by default.
 */
public class BigKeyPartitioner implements Partitioner {
  public static final String BIG_KEY_CONFIG = "big.key";

  private String bigKey;

  @Override
  public void configure(Map<String, ?> configs) {
    // configure() receives every producer property, including our custom big.key
    final Object value = configs.get(BIG_KEY_CONFIG);
    if (value == null) {
      throw new ConfigException(BIG_KEY_CONFIG + " must be set when using " + getClass().getSimpleName());
    }
    bigKey = value.toString();
  }

  @Override
  public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
    final int numPartitions = cluster.partitionCountForTopic(topic);
    if (numPartitions < 2) {
      throw new InvalidRecordException(topic + " needs at least 2 partitions for BigKeyPartitioner");
    }
    if (keyBytes == null || !(key instanceof String)) {
      throw new InvalidRecordException("Record did not have a valid key");
    }
    if (key.equals(bigKey)) {
      return 0; // the hot key always gets partition 0 to itself
    }
    // Every other key: hash over partitions 1..n-1. Utils.toPositive, not Math.abs, which returns a
    // negative number for Integer.MIN_VALUE.
    return Utils.toPositive(Utils.murmur2(keyBytes)) % (numPartitions - 1) + 1;
  }

  @Override
  public void close() {}
}
