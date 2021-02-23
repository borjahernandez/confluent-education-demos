# Overview

Set of short demos that can be quickly performed during a class to reinforce the learning of key concepts or to answer questions by showing how to do it. The idea is to have easy access to all the demos regardless of the class that is being given.

The demos use a Kafka cluster in Confluent Cloud and local Java Producers and Consumers.

# Prerequisites

* Java 1.8 or higher to run the demo application
* [Gradle](https://gradle.org/install) to compile the demo applications
* Create a local file at `$HOME/.confluent/java_ccloud.config` with the configuration parameters to connect to your Kafka cluster in Confluent Cloud. Check the template file [java_ccloud.config](java_ccloud.config)
* [Confluent Cloud CLI](https://docs.confluent.io/ccloud-cli/current/install.html)
* IDE to show the code of the local Java Producers and Consumers to students (if required)

# Deployment Considerations

* If the demo requires the use of a ksqlDB app, consider creating the ksqlDB app in advance (before starting the class or during a break) since Confluent Cloud takes around 10 minutes to provide resources to this new app
* Once the demo is finished, please do some housekeeping deleting the topics and ksqlDB apps that you created during the demo

# List of demos

| Demo | Description | Course
| ---- | ----------- | ------
| [change-number-partitions-ksqldb](change-number-partitions-ksqldb/) | Showing how to increase the number of partitions of a topic keeping messages with the same key in the same partition (workaround: migrating the data to a new topic using ksqlDB) | ADM DEV STR
| [change-serialization-format-ksqldb](change-serialization-format-ksqldb/) | In this Demo, a producer writes data in Kafka from a CSV file using the StringSerializer. A ksqlDB app is created to convert the data from String to JSON format, and then the data is consumed to display it in JSON format | DEV STR
| [implement-custom-partitioner](implement-custom-partitioner/) | Showing the consequences of producing data with a "hot" key using the Default Partitioner and how that problem can be resolved by using a Custom Partitioner | ADM DEV STR
