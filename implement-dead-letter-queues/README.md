# Implementing dead letter queues in Kafka clients

**The question this answers:** "What happens when one bad record turns up, and how do we stop it
taking the whole pipeline down?"

A record that cannot be processed should go to a **dead letter queue** (DLQ), a separate topic
where it can be inspected, fixed and replayed, while everything else keeps flowing. This demo shows
the pattern on both sides:

| Project | Does |
| --- | --- |
| [`producer`](producer/src/main/java/clients/Producer.java) | The naive version: the first bad input line throws and the producer dies |
| [`producer-dlq`](producer-dlq/src/main/java/clients/Producer.java) | Sends a line it cannot parse to the DLQ topic, with the error in the headers, and carries on |
| [`consumer-dlq`](consumer-dlq/src/main/java/clients/Consumer.java) | Catches a record it cannot deserialize (a "poison pill"), sends its raw bytes to the DLQ and seeks past it |

## The data

[`data/users-data-incr.csv`](data/users-data-incr.csv): 1,000 fake users, pipe-separated:
`Id|FirstName|LastName|Email|Birthday|RegistrationTimestamp|ActiveAccount`. Each line becomes a
Protobuf `User` ([`proto/user.proto`](proto/user.proto)) keyed by Id. The Java class is generated from
the schema at build time.

A few lines are deliberately broken:

```
id=6|Colby|Winters|in.tempus.eu@vel.org|May 23, 1988|1583641915|false      <- Id is not a number
11|Dacey|Pruitt|accumsan.neque.et@lacusUtnec.ca|Dec 16, 1983||false        <- empty timestamp
16|Chastity|Brewer|ornare@velfaucibusid.edu|Sep 6, 1967|1558865475|       <- empty ActiveAccount
```

## Steps

Commands run from the repo root. Add `-Pconfig=config/ccloud.properties` to use Confluent Cloud;
this demo also needs the Schema Registry settings in that file.

**1. Create the topics.** One partition for the DLQ keeps its records in the order they failed.

```bash
docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --topic user-topic --partitions 6
docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --topic user-topic-dlq --partitions 1
# Confluent Cloud: confluent kafka topic create <topic> --partitions <n>
```

**2. Run the naive producer.** It sends users 1 to 5, then dies on line 6:

```bash
./gradlew :implement-dead-letter-queues:producer:run
```

```
Message sent: 5|Acton|Pitts|Nunc.quis.arcu@pretiumneque.org|Nov 6, 1986|1532799850|false
Exception in thread "main" java.lang.NumberFormatException: For input string: "id=6"
```

**3. Run the producer with a DLQ.** Walk through the `catch` block in `producer-dlq`: a second
producer with a `StringSerializer` (the bad line is not a valid `User`), the original line kept as
the value, and the diagnosis in `error.class`, `error.message` and `source.line` headers.

```bash
./gradlew :implement-dead-letter-queues:producer-dlq:run
```

```
Message sent: 5|Acton|Pitts|Nunc.quis.arcu@pretiumneque.org|Nov 6, 1986|1532799850|false
INVALID MESSAGE sent to user-topic-dlq: id=6|Colby|Winters|...  (java.lang.NumberFormatException: For input string: "id=6")
Message sent: 7|Allegra|Ortega|nisi.Cum.sociis@ategestasa.co.uk|Dec 10, 1969|1609210804|false
```

**4. Look at the DLQ**

```bash
docker compose exec kafka kafka-console-consumer --bootstrap-server kafka:29092 --topic user-topic-dlq --from-beginning --formatter-property print.headers=true
# Confluent Cloud: open user-topic-dlq in the Cloud Console to see the headers
```

```
error.class:java.lang.NumberFormatException,error.message:For input string: "id=6",source.line:6	id=6|Colby|Winters|in.tempus.eu@vel.org|May 23, 1988|1583641915|false
```

**5. The consumer side.** Start the consumer (terminal 2); it prints every `User`:

```bash
./gradlew :implement-dead-letter-queues:consumer-dlq:run
```

Now write something to `user-topic` that is not Protobuf at all (terminal 3):

```bash
echo "not-a-user" | docker compose exec -T kafka kafka-console-producer --bootstrap-server kafka:29092 --topic user-topic
# Confluent Cloud: confluent kafka topic produce user-topic, then type not-a-user
```

Without special handling `poll()` would throw on that record every time it is called, and the
consumer would be stuck at that offset for good. Here it catches `RecordDeserializationException`,
which carries the record's partition, offset and raw bytes, copies them to the DLQ and seeks one
past the bad record:

```
POISON PILL at user-topic-3 offset 12 sent to user-topic-dlq: Error deserializing Protobuf message for id ...
```

## Clean up

Stop the clients with Ctrl+C and delete `user-topic`, `user-topic-dlq`, and the `user-topic-value`
subject in Schema Registry.
