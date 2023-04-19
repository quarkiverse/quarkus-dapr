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
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.Produces;
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
 * CloudEventReader
 *
 * @author naah69
 * @date 2022/4/25 10:04 AM
 */
@Provider
@Produces(CloudEvent.CONTENT_TYPE)
public class CloudEventReader implements MessageBodyReader<CloudEvent> {

    private static final ObjectMapper OBJECT_MAPPER = CDI.current().select(ObjectMapper.class).get();
    private static final DaprConfig DAPR_CONFIG = CDI.current().select(DaprConfig.class).get();
    private static final Map<Type, JavaType> TYPE_CACHE = new ConcurrentHashMap<>();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == CloudEvent.class;
    }

    @Override
    public CloudEvent readFrom(Class<CloudEvent> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        JavaType valueType = TYPE_CACHE.computeIfAbsent(genericType,
                a -> OBJECT_MAPPER.getTypeFactory().constructType(genericType));
        JsonNode jsonNode = OBJECT_MAPPER.readTree(entityStream);
        return getCloudEvent(jsonNode, valueType);

    }

    private static CloudEvent getCloudEvent(JsonNode jsonNode, JavaType valueType)
            throws IOException {
        String dataContentType = jsonNode.get("datacontenttype").asText();
        switch (dataContentType) {
            case MediaType.APPLICATION_JSON:
                return OBJECT_MAPPER.treeToValue(jsonNode, valueType);
            case MediaType.TEXT_PLAIN:
                String data = jsonNode.get("data").asText();
                return OBJECT_MAPPER.readValue(data, valueType);
            case MediaType.APPLICATION_OCTET_STREAM:
                byte[] binaryData = jsonNode.get("data_base64").binaryValue();
                String pubsubname = jsonNode.get("pubsubname").asText();
                String rawPayload = Optional.ofNullable(DAPR_CONFIG.pubSub.get(pubsubname))
                        .map(a -> a.consumeMetadata)
                        .map(a -> a.get("rawPayload"))
                        .orElse("");
                if (Objects.equals("true", rawPayload)) {
                    JsonNode subJsonNode = OBJECT_MAPPER.readTree(binaryData);
                    return getCloudEvent(subJsonNode, valueType);
                }
                return OBJECT_MAPPER.readValue(binaryData, valueType);
            default:
                throw new NotSupportedException("can't read unknown cloud event content type: " + dataContentType);
        }
    }

}
