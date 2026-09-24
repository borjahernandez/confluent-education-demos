-- Copy every record of five-partitions-topic into a new topic with 10 partitions.
-- Declaring the key column is what keeps records with the same key together: without it
-- ksqlDB drops the key and the output records are spread with no key at all.
SET 'auto.offset.reset' = 'earliest';

CREATE STREAM input_stream (country VARCHAR KEY, message VARCHAR)
  WITH (KAFKA_TOPIC='five-partitions-topic', KEY_FORMAT='KAFKA', VALUE_FORMAT='KAFKA');

CREATE STREAM output_stream
  WITH (KAFKA_TOPIC='ten-partitions-topic', PARTITIONS=10) AS
  SELECT * FROM input_stream
  EMIT CHANGES;
