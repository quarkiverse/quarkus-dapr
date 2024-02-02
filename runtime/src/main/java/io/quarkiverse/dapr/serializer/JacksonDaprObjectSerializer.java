package io.quarkiverse.dapr.serializer;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.MessageLite;

import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;

/**
 * JacksonDaprObjectSerializer
 *
 * @author naah69
 * @date 2022/4/19 12:52 PM
 */
public class JacksonDaprObjectSerializer implements DaprObjectSerializer {

    private final ObjectMapper objectMapper;

    public JacksonDaprObjectSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(Object state) throws IOException {
        if (state == null) {
            return null;
        }

        if (state.getClass() == Void.class) {
            return null;
        }

        // Have this check here to be consistent with deserialization (see deserialize() method below).
        if (state instanceof byte[]) {
            return (byte[]) state;
        }

        // Proto buffer class is serialized directly.
        if (state instanceof MessageLite) {
            return ((MessageLite) state).toByteArray();
        }

        // Not string, not primitive, so it is a complex type: we use JSON for that.
        return this.objectMapper.writeValueAsBytes(state);
    }

    @Override
    public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
        return this.objectMapper.readValue(data, this.objectMapper.constructType(type.getType()));
    }

    @Override
    public String getContentType() {
        return "application/json";
    }
}
