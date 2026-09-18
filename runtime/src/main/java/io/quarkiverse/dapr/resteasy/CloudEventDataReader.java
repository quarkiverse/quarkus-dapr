package io.quarkiverse.dapr.resteasy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
 * This allows resource methods to work when Dapr sends
 * {@code Content-Type: application/cloudevents+json}.
 */
@Singleton
@Provider
@Consumes(CloudEvent.CONTENT_TYPE)
public class CloudEventDataReader implements MessageBodyReader<Object> {

    private final ObjectMapper objectMapper;
    private final DaprConfig daprConfig;
    private final Map<Type, JavaType> typeCache = new ConcurrentHashMap<>();

    @Inject
    public CloudEventDataReader(ObjectMapper objectMapper, DaprConfig daprConfig) {
        this.objectMapper = objectMapper;
        this.daprConfig = daprConfig;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // Let CloudEventReader handle CloudEvent<T> endpoints.
        return type != CloudEvent.class;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        JavaType targetType = typeCache.computeIfAbsent(genericType,
                a -> objectMapper.getTypeFactory().constructType(genericType));

        JsonNode cloudEventNode = objectMapper.readTree(entityStream);
        JsonNode dataNode = cloudEventNode.get("data");
        JsonNode base64Node = cloudEventNode.get("data_base64");

        if (dataNode == null && base64Node == null) {
            return objectMapper.treeToValue(cloudEventNode, targetType);
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
                return objectMapper.treeToValue(dataNode, targetType);
            case MediaType.TEXT_PLAIN:
                if (dataNode == null || dataNode.isNull()) {
                    return null;
                }
                String dataText = dataNode.asText();
                if (Objects.equals(String.class, targetType.getRawClass())) {
                    return dataText;
                }
                return objectMapper.readValue(dataText, targetType);
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
                String rawPayload = Optional.ofNullable(daprConfig.pubSub().get(pubsubname))
                        .map(a -> a.consumeMetadata())
                        .map(a -> a.get("rawPayload"))
                        .orElse("");
                if (Objects.equals("true", rawPayload)) {
                    JsonNode payloadNode = objectMapper.readTree(binaryData);
                    return objectMapper.treeToValue(payloadNode, targetType);
                }
                return objectMapper.readValue(binaryData, targetType);
            default:
                throw new NotSupportedException("can't read unknown cloud event content type: " + dataContentType);
        }
    }
}
