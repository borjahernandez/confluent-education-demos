package clients;

import com.google.protobuf.Message;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import org.apache.kafka.common.errors.SerializationException;


public class CustomKafkaProtobufDeserializer<T extends Message> extends KafkaProtobufDeserializer<T> {

    @Override
    protected Object deserialize(boolean includeSchemaAndVersion, String topic, Boolean isKey, byte[] payload) throws SerializationException {
        try {
            return super.deserialize(includeSchemaAndVersion, topic, isKey, payload);
        } catch (SerializationException e) {
            return null;
        }
    }
}
