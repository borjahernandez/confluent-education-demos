# Overview

Set of short demos that can be quickly performed during a class to reinforce the learning of key concepts or to answer questions by showing how to do it. The idea is to have easy access to all the demos regardless of the class that is being given.
Produce messages to and consume messages from a Kafka cluster using the Java Producer and Consumer, and Kafka Streams API.


# Prerequisites

* Java 1.8 or higher to run the demo application
* Gradle to compile the demo applications
* Create a local file (e.g. at `$HOME/.confluent/java_ccloud.config`) with configuration parameters to connect to your Kafka cluster in Confluent Cloud. Check the template file [java_ccloud.config](https://github.com/borjahernandez/confluent-education-demos/blob/main/java_ccloud.config)
* [Confluent Cloud CLI](https://docs.confluent.io/ccloud-cli/current/install.html)
* IDE to show the code of local producers/consumers to students (if required)

# Deployment Considerations

* If the demo requires the use of a ksqlDB app, consider creating the ksqlDB app in advance (before starting the class or during a break) since Confluent Cloud takes around 10 minutes to provide resources to this new app
* Once the demo is finished, please do some housekeeping deleting the topics and ksqlDB apps that you created during the demo
