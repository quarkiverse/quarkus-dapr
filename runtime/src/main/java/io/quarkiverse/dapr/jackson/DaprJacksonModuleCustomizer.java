package io.quarkiverse.dapr.jackson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Singleton;

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
        // 配置[忽略未知字段]
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 空对象可序列化
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // null属性不序列化
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 能解析注释符
        objectMapper.enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature());

        // 能解析注释
        objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        // 解析单引号
        objectMapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

        // 配置[时间类型转换]
        JavaTimeModule timeModule = new JavaTimeModule();
        // LocalDateTime序列化与反序列化
        timeModule.addSerializer(new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        // LocalDate序列化与反序列化
        timeModule.addSerializer(new LocalDateSerializer(DATE_FORMATTER));
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));
        // LocalTime序列化与反序列化
        timeModule.addSerializer(new LocalTimeSerializer(TIME_FORMATTER));
        timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));
        objectMapper.registerModule(timeModule);
    }
}
