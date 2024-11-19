package com.learn.summer.web.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class JsonUtils {

    public static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        // 确保序列化过程中包含所有字段
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // 关闭一些特性
        // 忽略反序列化过程中遇到的未知字段
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 允许序列化空对象
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 使用 ISO-8601 格式序列化日期
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public static String writeJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        }catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeJson(Writer writer, Object obj) throws IOException {
        try {
            OBJECT_MAPPER.writeValue(writer, obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeJson(OutputStream output, Object obj) throws IOException {
        try {
            OBJECT_MAPPER.writeValue(output, obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(Reader reader, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(reader, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public static <T> T readJson(Reader reader, TypeReference<T> ref) {
        try {
            return OBJECT_MAPPER.readValue(reader, ref);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public static <T> T readJson(InputStream input, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(input, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public static <T> T readJson(InputStream input, TypeReference<T> ref) {
        try {
            return OBJECT_MAPPER.readValue(input, ref);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public static <T> T readJson(byte[] src, TypeReference<T> ref) {
        try {
            return OBJECT_MAPPER.readValue(src, ref);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(String str, TypeReference<T> ref) {
        try {
            return OBJECT_MAPPER.readValue(str, ref);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Map<String, Object> readJsonAsMap(String str) {
        try {
            return OBJECT_MAPPER.readValue(str, new TypeReference<HashMap<String, Object>>() {
            });
        }catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
