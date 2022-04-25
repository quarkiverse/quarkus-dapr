package io.quarkiverse.dapr.resteasy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dapr.client.domain.CloudEvent;

/**
 * CloudEventReader
 *
 * @author nayan
 * @date 2022/4/25 10:04 AM
 */
@Provider
@Produces(CloudEvent.CONTENT_TYPE)
public class CloudEventReader<T> implements MessageBodyReader<CloudEvent<T>> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == CloudEvent.class;
    }

    @Override
    public CloudEvent<T> readFrom(Class<CloudEvent<T>> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        byte[] bytes = getBytes(entityStream);
        if (bytes.length == 0) {
            throw new NoContentException("Cannot create JsonObject");
        }
        ObjectMapper objectMapper = CDI.current().select(ObjectMapper.class).get();
        return objectMapper.readValue(bytes, type);
    }

    private static byte[] getBytes(InputStream entityStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = entityStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }
}
