package io.quarkiverse.dapr.jackson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Singleton
public class DaprJacksonModuleCustomizer implements JsonMapperBuilderCustomizer {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

    @Override
    public void customize(JsonMapper.Builder builder) {
        // 配置[忽略未知字段]
        builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 空对象可序列化
        builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // null属性不序列化
        builder.changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL));

        // allow non-public record constructors (e.g., nested records)
        builder.changeDefaultVisibility(
                visibility -> visibility.withVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY));

        // 能解析注释符
        builder.enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);

        // 能解析注释
        builder.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS);

        // 解析单引号
        builder.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES);

        // 配置[时间类型转换]
        SimpleModule timeModule = new SimpleModule();
        // LocalDateTime序列化与反序列化
        timeModule.addSerializer(new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        // LocalDate序列化与反序列化
        timeModule.addSerializer(new LocalDateSerializer(DATE_FORMATTER));
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));
        // LocalTime序列化与反序列化
        timeModule.addSerializer(new LocalTimeSerializer(TIME_FORMATTER));
        timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));
        builder.addModule(timeModule);
    }
}
