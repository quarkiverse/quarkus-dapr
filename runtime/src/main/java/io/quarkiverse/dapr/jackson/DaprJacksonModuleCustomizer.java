package io.quarkiverse.dapr.jackson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class DaprJacksonModuleCustomizer implements ObjectMapperCustomizer {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

    @Override
    public void customize(ObjectMapper objectMapper) {
        // Configure: ignore unknown properties
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Allow empty beans to be serialized (don't fail on empty beans)
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Do not serialize null properties
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Allow backslash escaping of any character
        objectMapper.enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature());

        // Allow comments in JSON
        objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        // Allow single quotes in JSON
        objectMapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

        // Configure time type conversions
        JavaTimeModule timeModule = new JavaTimeModule();
        // LocalDateTime serialization and deserialization
        timeModule.addSerializer(new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        // LocalDate serialization and deserialization
        timeModule.addSerializer(new LocalDateSerializer(DATE_FORMATTER));
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));
        // LocalTime serialization and deserialization
        timeModule.addSerializer(new LocalTimeSerializer(TIME_FORMATTER));
        timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));
        objectMapper.registerModule(timeModule);
    }
}
