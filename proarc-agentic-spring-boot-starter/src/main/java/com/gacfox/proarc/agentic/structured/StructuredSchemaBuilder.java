package com.gacfox.proarc.agentic.structured;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gacfox.proarc.agentic.model.openai.Function;
import com.gacfox.proarc.agentic.model.openai.Parameters;
import com.gacfox.proarc.agentic.model.openai.Property;
import com.gacfox.proarc.agentic.model.openai.Tool;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构化输出Schema构建器
 */
class StructuredSchemaBuilder {

    Tool buildTool(Class<?> responseType, String toolName) {
        Map<String, Property> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Field field : responseType.getDeclaredFields()) {
            if (shouldSkip(field)) {
                continue;
            }
            String name = resolveFieldName(field);
            properties.put(name, Property.builder()
                    .type(resolveJsonType(field.getType()))
                    .description(resolveDescription(field))
                    .build());
            if (field.isAnnotationPresent(StructuredRequired.class)) {
                required.add(name);
            }
        }

        if (properties.isEmpty()) {
            throw new IllegalArgumentException("Structured response type has no available fields: "
                    + responseType.getName());
        }
        if (required.isEmpty()) {
            required.addAll(properties.keySet());
        }

        Function function = Function.builder()
                .name(toolName)
                .description(resolveDescription(responseType))
                .parameters(Parameters.builder()
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
        return Tool.builder().function(function).build();
    }

    private boolean shouldSkip(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers)
                || Modifier.isTransient(modifiers)
                || field.isSynthetic()
                || field.isAnnotationPresent(JsonIgnore.class);
    }

    private String resolveFieldName(Field field) {
        StructuredName structuredName = field.getAnnotation(StructuredName.class);
        if (structuredName != null && StringUtils.hasText(structuredName.value())) {
            return structuredName.value();
        }
        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && StringUtils.hasText(jsonProperty.value())) {
            return jsonProperty.value();
        }
        return field.getName();
    }

    private String resolveDescription(Field field) {
        StructuredDescription description = field.getAnnotation(StructuredDescription.class);
        if (description != null && StringUtils.hasText(description.value())) {
            return description.value();
        }
        return field.getName();
    }

    private String resolveDescription(Class<?> responseType) {
        StructuredDescription description = responseType.getAnnotation(StructuredDescription.class);
        if (description != null && StringUtils.hasText(description.value())) {
            return description.value();
        }
        return "Return the structured output for " + responseType.getSimpleName();
    }

    private String resolveJsonType(Class<?> type) {
        if (type == String.class || type == Character.class || type == char.class || type.isEnum()) {
            return "string";
        }
        if (type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Short.class || type == short.class
                || type == Byte.class || type == byte.class) {
            return "integer";
        }
        if (type == Double.class || type == double.class
                || type == Float.class || type == float.class) {
            return "number";
        }
        if (type == Boolean.class || type == boolean.class) {
            return "boolean";
        }
        if (List.class.isAssignableFrom(type) || type.isArray()) {
            return "array";
        }
        if (Map.class.isAssignableFrom(type) || !type.isPrimitive() && !type.getName().startsWith("java.")) {
            return "object";
        }
        throw new IllegalArgumentException("Unsupported structured field type: " + type.getName());
    }
}
