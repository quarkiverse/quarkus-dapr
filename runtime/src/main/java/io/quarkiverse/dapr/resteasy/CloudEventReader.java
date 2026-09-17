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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dapr.client.domain.CloudEvent;
import io.quarkiverse.dapr.config.DaprConfig;

/**
 * CloudEventReader
 * <p>
 * Implements {@link ServerMessageBodyReader} so that RESTEasy Reactive does not need to reflectively look up the
 * resource method to obtain parameter annotations. A plain {@code MessageBodyReader} triggers
 * {@code Class.getMethod(...)} at request time, which fails in native mode because resource methods that only take a
 * {@code CloudEvent} parameter are not registered for reflection.
 *
 * @author naah69
 * @date 2022/4/25 10:04 AM
 */
@Provider
@Produces(CloudEvent.CONTENT_TYPE)
public class CloudEventReader implements ServerMessageBodyReader<CloudEvent> {

    private static ObjectMapper OBJECT_MAPPER;
    private static DaprConfig DAPR_CONFIG;
    private static final Map<Type, JavaType> TYPE_CACHE = new ConcurrentHashMap<>();

    public CloudEventReader() {
        if (OBJECT_MAPPER == null) {
            OBJECT_MAPPER = CDI.current().select(ObjectMapper.class).get();
        }
        if (DAPR_CONFIG == null) {
            DAPR_CONFIG = CDI.current().select(DaprConfig.class).get();
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod,
            MediaType mediaType) {
        return type == CloudEvent.class;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == CloudEvent.class;
    }

    @Override
    public CloudEvent readFrom(Class<CloudEvent> type, Type genericType, MediaType mediaType,
            ServerRequestContext context) throws WebApplicationException, IOException {
        return read(genericType, context.getInputStream());
    }

    @Override
    public CloudEvent readFrom(Class<CloudEvent> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return read(genericType, entityStream);
    }

    private static CloudEvent read(Type genericType, InputStream entityStream) throws IOException {
        JavaType valueType = TYPE_CACHE.computeIfAbsent(genericType,
                a -> OBJECT_MAPPER.getTypeFactory().constructType(genericType));
        JsonNode jsonNode = OBJECT_MAPPER.readTree(entityStream);
        return getCloudEvent(jsonNode, valueType);
    }

    private static CloudEvent getCloudEvent(JsonNode jsonNode, JavaType valueType)
            throws IOException {
        String dataContentType = Optional.ofNullable(jsonNode.get("datacontenttype"))
                .map(JsonNode::asText)
                .orElse(MediaType.APPLICATION_JSON);
        if (isOctetStream(dataContentType)) {
            byte[] binaryData = jsonNode.get("data_base64").binaryValue();
            String pubsubname = jsonNode.get("pubsubname").asText();
            String rawPayload = Optional.ofNullable(DAPR_CONFIG.pubSub().get(pubsubname))
                    .map(a -> a.consumeMetadata())
                    .map(a -> a.get("rawPayload"))
                    .orElse("");
            if (Objects.equals("true", rawPayload)) {
                JsonNode subJsonNode = OBJECT_MAPPER.readTree(binaryData);
                return getCloudEvent(subJsonNode, valueType);
            }
            return OBJECT_MAPPER.readValue(binaryData, valueType);
        }
        return OBJECT_MAPPER.treeToValue(jsonNode, valueType);
    }

    private static boolean isOctetStream(String dataContentType) {
        if (dataContentType == null) {
            return false;
        }
        try {
            MediaType mediaType = MediaType.valueOf(dataContentType);
            return mediaType.isCompatible(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (IllegalArgumentException ex) {
            return dataContentType.startsWith(MediaType.APPLICATION_OCTET_STREAM);
        }
    }

}
