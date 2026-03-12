package io.quarkiverse.dapr.resteasy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dapr.client.domain.CloudEvent;
import io.quarkiverse.dapr.config.DaprConfig;

/**
 * Reads a Dapr CloudEvent and deserializes its {@code data} into the requested JAX-RS parameter type.
 *
 * This allows resource methods like {@code public void consume(Order order)} to work when Dapr sends
 * {@code Content-Type: application/cloudevents+json}.
 */
@Provider
@Consumes(CloudEvent.CONTENT_TYPE)
public class CloudEventDataReader implements MessageBodyReader<Object> {

    private static ObjectMapper OBJECT_MAPPER;
    private static DaprConfig DAPR_CONFIG;
    private static final Map<Type, JavaType> TYPE_CACHE = new ConcurrentHashMap<>();

    public CloudEventDataReader() {
        if (OBJECT_MAPPER == null) {
            OBJECT_MAPPER = CDI.current().select(ObjectMapper.class).get();
        }
        if (DAPR_CONFIG == null) {
            DAPR_CONFIG = CDI.current().select(DaprConfig.class).get();
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // Let CloudEventReader handle CloudEvent<T> endpoints.
        return type != CloudEvent.class;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        JavaType targetType = TYPE_CACHE.computeIfAbsent(genericType,
                a -> OBJECT_MAPPER.getTypeFactory().constructType(genericType));

        JsonNode cloudEventNode = OBJECT_MAPPER.readTree(entityStream);
        JsonNode dataNode = cloudEventNode.get("data");
        JsonNode base64Node = cloudEventNode.get("data_base64");

        if (dataNode == null && base64Node == null) {
            return OBJECT_MAPPER.treeToValue(cloudEventNode, targetType);
        }

        String dataContentType = Optional.ofNullable(cloudEventNode.get("datacontenttype"))
                .map(JsonNode::asText)
                .filter(s -> !s.isBlank())
                .orElse(MediaType.APPLICATION_JSON);

        switch (dataContentType) {
            case MediaType.APPLICATION_JSON:
                if (dataNode == null || dataNode.isNull()) {
                    return null;
                }
                return OBJECT_MAPPER.treeToValue(dataNode, targetType);
            case MediaType.TEXT_PLAIN:
                if (dataNode == null || dataNode.isNull()) {
                    return null;
                }
                String dataText = dataNode.asText();
                if (Objects.equals(String.class, targetType.getRawClass())) {
                    return dataText;
                }
                return OBJECT_MAPPER.readValue(dataText, targetType);
            case MediaType.APPLICATION_OCTET_STREAM:
                if (base64Node == null || base64Node.isNull()) {
                    return null;
                }
                byte[] binaryData = base64Node.binaryValue();
                if (Objects.equals(byte[].class, targetType.getRawClass())) {
                    return binaryData;
                }
                String pubsubname = Optional.ofNullable(cloudEventNode.get("pubsubname"))
                        .map(JsonNode::asText)
                        .orElse("");
                String rawPayload = Optional.ofNullable(DAPR_CONFIG.pubSub().get(pubsubname))
                        .map(a -> a.consumeMetadata())
                        .map(a -> a.get("rawPayload"))
                        .orElse("");
                if (Objects.equals("true", rawPayload)) {
                    JsonNode payloadNode = OBJECT_MAPPER.readTree(binaryData);
                    return OBJECT_MAPPER.treeToValue(payloadNode, targetType);
                }
                return OBJECT_MAPPER.readValue(binaryData, targetType);
            default:
                throw new NotSupportedException("can't read unknown cloud event content type: " + dataContentType);
        }
    }
}
