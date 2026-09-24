# Implementing a custom partitioner

**The question this answers:** "One of our keys is far bigger than the others. What happens, and
what can we do about it?"

The default partitioner sends a key to `murmur2(key) % partitions`. When one key carries a large
share of the traffic, its partition, and the one consumer that owns it, falls behind while the rest
sit idle. A custom `Partitioner` can give the hot key a partition of its own and spread everything
else over the others.

## The data

[`producer/data/inputData.csv`](producer/data/inputData.csv): 100 fake rows of
`FirstName,LastName,Email,RegistrationDate,Country`, keyed by Country. **United States is about a
fifth of all records**: that is the hot key.

![Dataset](../res/dataset.png)

## Steps

Commands run from the repo root. Add `-Pconfig=config/ccloud.properties` to use Confluent Cloud.

**1. Create two topics with 5 partitions each**

```bash
docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --topic default-topic --partitions 5
docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --topic custom-topic --partitions 5
# Confluent Cloud: confluent kafka topic create <topic> --partitions 5
```

**2. Produce with the default partitioner** (terminal 1)

```bash
PARTITIONER=default TOPIC=default-topic ./gradlew :implement-custom-partitioner:producer:run
```

**3. Consume with a group of 5** (terminal 2)

```bash
NUM_CONSUMERS=5 TOPIC=default-topic ./gradlew :implement-custom-partitioner:consumer-group:run
```

**4. Look at the lag** of `consumer-group-demo-partitioner`. One partition, the one United States
hashes to, has far more lag than the rest, because it carries the hot key *plus* its share of the
other countries.

```bash
docker compose exec kafka kafka-consumer-groups --bootstrap-server kafka:29092 --describe --group consumer-group-demo-partitioner
# Confluent Cloud: the Consumers tab of the cluster
```

**5. Walk through the code.** Stop the producer (Ctrl+C) and open:

- [`Producer.java`](producer/src/main/java/clients/Producer.java): `PARTITIONER_CLASS_CONFIG` plugs in
  the custom class, and the custom `big.key` property travels in the same `Properties` object.
- [`BigKeyPartitioner.java`](producer/src/main/java/clients/BigKeyPartitioner.java): `configure()`
  receives every producer property, which is how it learns the hot key. `partition()` sends the hot
  key to partition 0 and hashes everything else over partitions 1 to n-1.

**6. Produce with the custom partitioner**

```bash
PARTITIONER=custom TOPIC=custom-topic ./gradlew :implement-custom-partitioner:producer:run
```

**7. Move the consumer group over** (Ctrl+C the old one first)

```bash
NUM_CONSUMERS=5 TOPIC=custom-topic ./gradlew :implement-custom-partitioner:consumer-group:run
```

Check the lag again: partition 0 now holds United States and nothing else, and the lag is even
across the other four.

**Discussion point:** this only helps if one consumer can keep up with the hot key on its own. If
it cannot, the key itself needs splitting (for example, salting it), which gives up per-key ordering.

## Clean up

Stop the clients with Ctrl+C and delete `default-topic` and `custom-topic`.
