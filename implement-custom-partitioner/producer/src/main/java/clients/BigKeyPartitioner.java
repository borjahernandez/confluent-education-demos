package clients;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.InvalidRecordException;
import org.apache.kafka.common.utils.Utils;

import java.util.Map;

public class BigKeyPartitioner implements Partitioner {
    private String bigKey;

    public void configure(Map<String, ?> configs) {
        bigKey = configs.get("big.key").toString();
    }
    public void close() {}
    public void onNewBatch(String topic, Cluster cluster, int prevPartition) {}

    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        int numPartitions = cluster.partitionCountForTopic(topic);

        if ((keyBytes == null) || (!(key instanceof String))) {
            throw new InvalidRecordException("Record did not have a valid Key");
        }
        if (key.equals(this.bigKey)) {
            return 0;  // This key will always go to Partition 0
        }
        // Other records will go to the rest of the Partitions using a hashing function
        return (Math.abs(Utils.murmur2(keyBytes)) % (numPartitions - 1)) + 1;
    }
}
