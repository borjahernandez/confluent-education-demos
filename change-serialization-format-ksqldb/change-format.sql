-- Read the CSV strings in csv-topic and write them back out as JSON to json-topic.
-- The key (Country) is declared too, so the JSON records keep the same key as the CSV ones.
SET 'auto.offset.reset' = 'earliest';

CREATE STREAM csv_stream (
    country_key VARCHAR KEY,
    first_name VARCHAR,
    last_name VARCHAR,
    email VARCHAR,
    registration_date VARCHAR,
    country VARCHAR
  ) WITH (KAFKA_TOPIC='csv-topic', KEY_FORMAT='KAFKA', VALUE_FORMAT='DELIMITED');

CREATE STREAM json_stream
  WITH (KAFKA_TOPIC='json-topic', VALUE_FORMAT='JSON', PARTITIONS=6) AS
  SELECT * FROM csv_stream
  EMIT CHANGES;
