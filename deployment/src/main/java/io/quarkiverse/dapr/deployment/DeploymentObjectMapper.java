package io.quarkiverse.dapr.deployment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeploymentObjectMapper {

    private static ObjectMapper OBJECT_MAPPER;

    public static ObjectMapper getInstance() {

        if (OBJECT_MAPPER == null) {
            OBJECT_MAPPER = new ObjectMapper();
            OBJECT_MAPPER.setDefaultPropertyInclusion(
                    JsonInclude.Include.NON_NULL);
        }

        return OBJECT_MAPPER;
    }
}
