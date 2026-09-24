# Changing the serialization format of a topic

**The question this answers:** "Our topic is full of CSV strings. Can we get JSON without touching
the producer?"

Yes: declare the shape of the data to ksqlDB once, and have it write a JSON copy continuously.

## The data

[`producer/data/inputData.csv`](producer/data/inputData.csv): 100 fake rows of
`FirstName,LastName,Email,RegistrationDate,Country`. The producer sends each row as a plain String,
keyed by Country.

![Dataset](../res/dataset.png)

## Steps

Commands run from the repo root. Add `-Pconfig=config/ccloud.properties` to use Confluent Cloud.

**1. Create `csv-topic` with 6 partitions**

```bash
docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --topic csv-topic --partitions 6
# Confluent Cloud: confluent kafka topic create csv-topic --partitions 6
```

**2. Start the producer** (terminal 1)

```bash
./gradlew :change-serialization-format-ksqldb:producer:run
```

**3. Look at the raw data** (terminal 2)

```bash
docker compose exec kafka kafka-console-consumer --bootstrap-server kafka:29092 --topic csv-topic --from-beginning
# Confluent Cloud: confluent kafka topic consume csv-topic --from-beginning
```

```
Cara,Martinez,Cum@ProinultricesDuis.edu,08/31/2019,Spain
Graiden,Myers,nec@orciquislectus.ca,05/17/2020,United States
```

**4. Convert it.** Run [`change-format.sql`](change-format.sql): locally as below, or paste it into
the ksqlDB editor in Confluent Cloud with `auto.offset.reset = Earliest`.

```bash
docker compose exec ksqldb-cli ksql http://ksqldb:8088 -f /demos/change-serialization-format-ksqldb/change-format.sql
```

`VALUE_FORMAT='DELIMITED'` is what tells ksqlDB to split the String on commas; the
`CREATE STREAM ... AS SELECT` then writes every record, past and future, as JSON.

**5. Consume the JSON**

```bash
docker compose exec kafka kafka-console-consumer --bootstrap-server kafka:29092 --topic json-topic --from-beginning | jq
# Confluent Cloud: confluent kafka topic consume json-topic --from-beginning | jq
```

```json
{
  "FIRST_NAME": "Brennan",
  "LAST_NAME": "Gates",
  "EMAIL": "et.lacinia.vitae@turpisAliquamadipiscing.net",
  "REGISTRATION_DATE": "01/03/2020",
  "COUNTRY": "France"
}
```

## Clean up

Stop the producer with Ctrl+C. Delete `csv-topic`, `json-topic`, the `*ksql_processing_log` topic,
and the ksqlDB cluster on Confluent Cloud.
