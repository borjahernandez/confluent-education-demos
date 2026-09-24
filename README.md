# Confluent Education Demos

[![CI](https://github.com/borjahernandez/confluent-education-demos/actions/workflows/ci.yml/badge.svg)](https://github.com/borjahernandez/confluent-education-demos/actions/workflows/ci.yml)

Short, self-contained Apache Kafka demos I use in class to make a concept stick, or to answer a
question by showing it rather than describing it. Each one takes 5 to 15 minutes, runs against a
local cluster or Confluent Cloud, and is tested end to end in CI on every push.

| Demo | What it shows | Course |
| --- | --- | --- |
| [Change the number of partitions](change-number-partitions-ksqldb/) | You cannot add partitions to a keyed topic without breaking key ordering, so migrate the data to a new topic with ksqlDB instead | ADM · DEV · STR |
| [Change the serialization format](change-serialization-format-ksqldb/) | Turn a topic of CSV strings into JSON with two ksqlDB statements | DEV · STR |
| [Custom partitioner](implement-custom-partitioner/) | What a "hot" key does to consumer lag, and how a custom `Partitioner` fixes it | ADM · DEV · STR |
| [Dead letter queues](implement-dead-letter-queues/) | Keep a producer *and* a consumer running when they meet a bad record, by routing it to a DLQ | ADM · DEV · STR |

## Requirements

- JDK 17 or newer. Gradle is not needed; the wrapper downloads it.
- Either **Docker** (for the local cluster) or a **Confluent Cloud** cluster and the
  [`confluent` CLI](https://docs.confluent.io/confluent-cli/current/install.html).

## Running against a local cluster

```bash
docker compose up -d        # Kafka (KRaft), Schema Registry and ksqlDB
./gradlew build             # compile everything once
```

Every client reads its connection settings from [`config/local.properties`](config/local.properties)
by default. Topics are not auto-created, as on Confluent Cloud, so each demo starts by creating them:

```bash
docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --topic <name> --partitions <n>
```

ksqlDB statements run from the CLI container, which has the repo mounted at `/demos`:

```bash
docker compose exec ksqldb-cli ksql http://ksqldb:8088
```

Clean up with `docker compose down -v`.

## Running against Confluent Cloud

```bash
cp config/ccloud.properties.template config/ccloud.properties   # then fill in the API keys
./gradlew :implement-custom-partitioner:producer:run -Pconfig=config/ccloud.properties
```

`config/ccloud.properties` is gitignored. Before class:

- Create the ksqlDB cluster in advance (during a break is fine). Provisioning takes a few minutes.
- Afterwards, delete the topics and the ksqlDB cluster you created for the demo.

## Layout

```
config/                     connection settings: local (docker compose) and Confluent Cloud template
common/                     the one helper every client shares: load the properties file, read env vars
<demo>/README.md            the script for the demo
<demo>/producer, consumer…  one Gradle project per client, run with ./gradlew :<demo>:<client>:run
scripts/ci/                 assertions the CI workflow uses to check each demo actually did its job
```

Built with Java 17, Gradle 9, Apache Kafka clients 4.3, Confluent Schema Registry serializers 8.3.
