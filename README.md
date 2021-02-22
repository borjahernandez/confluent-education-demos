# Overview

Set of short demos that can be quickly performed during a class to reinforce the learning of key concepts or to answer questions by showing how to do it. The idea is to have easy access to all the demos regardless of the class that is being given.

The demos use a Kafka cluster in Confluent Cloud and local Java Producers and Consumers.

# Prerequisites

* Java 1.8 or higher to run the demo application
* Gradle to compile the demo applications
* Create a local file (e.g. at `$HOME/.confluent/java_ccloud.config`) with configuration parameters to connect to your Kafka cluster in Confluent Cloud. Check the template file [java_ccloud.config](java_ccloud.config)
* [Confluent Cloud CLI](https://docs.confluent.io/ccloud-cli/current/install.html)
* IDE to show the code of local producers/consumers to students (if required)

# Deployment Considerations

* If the demo requires the use of a ksqlDB app, consider creating the ksqlDB app in advance (before starting the class or during a break) since Confluent Cloud takes around 10 minutes to provide resources to this new app
* Once the demo is finished, please do some housekeeping deleting the topics and ksqlDB apps that you created during the demo

# Data Pipelines

| Demo                                       | Description                                        | Course
| ------------------------------------------ | -------------------------------------------------- | ------------
| [change-serialization-format-ksqldb](change-serialization-format-ksqldb/) | Showing how to increase the number of partitions of a topic keeping messages with the same key in the same partition (workaround: migrating the data to a new topic using ksqlDB) | ADM, DEV, STR
| [Connect and Kafka Streams](connect-streams-pipeline/README.md) |   [Y](connect-streams-pipeline/README.md)   |   N
