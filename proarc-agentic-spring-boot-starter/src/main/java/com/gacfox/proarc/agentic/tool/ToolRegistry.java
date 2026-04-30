package com.gacfox.proarc.agentic.tool;

import com.gacfox.proarc.agentic.model.openai.Function;
import com.gacfox.proarc.agentic.model.openai.Parameters;
import com.gacfox.proarc.agentic.model.openai.Property;
import com.gacfox.proarc.agentic.model.openai.Tool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体工具注册中心
 */
@Slf4j
public class ToolRegistry {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Map<String, ToolDefinition> registry = new ConcurrentHashMap<>();

    /**
     * 注册智能体工具
     *
     * @param bean 智能体工具实例
     */
    public void register(Object bean) {
        Class<?> clazz = bean.getClass();

        for (Method method : clazz.getDeclaredMethods()) {
            AgenticTool agenticTool = method.getAnnotation(AgenticTool.class);
            if (agenticTool == null) {
                continue;
            }

            String toolName = agenticTool.name();
            if (registry.containsKey(toolName)) {
                throw new IllegalStateException(
                        "Duplicate agentic tool name detected: '" + toolName + "'. "
                                + "Tool names must be globally unique. "
                                + "Conflicting definition found in " + clazz.getName() + "." + method.getName() + "()");
            }

            validateParameterTypes(method);
            String jsonSchema = buildJsonSchema(agenticTool, method);
            method.setAccessible(true);
            registry.put(toolName, new ToolDefinition(toolName, bean, method, jsonSchema));
            log.info("Registered agentic tool: {} -> {}.{}", toolName, clazz.getSimpleName(), method.getName());
        }
    }

    /**
     * 获取所有已注册的工具定义
     *
     * @return 工具定义列表
     */
    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(registry.values());
    }

    /**
     * 获取智能体工具信息
     *
     * @param toolName 智能体工具名称
     * @return 智能体工具信息
     */
    public ToolDefinition getAgenticTool(String toolName) {
        return registry.get(toolName);
    }

    private void validateParameterTypes(Method method) {
        for (Parameter param : method.getParameters()) {
            String jsonType = resolveJsonType(param.getType());
            if (jsonType == null) {
                throw new IllegalArgumentException(
                        "Unsupported parameter type in tool method " + method.getName()
                                + ": " + param.getType().getName() + " " + param.getName());
            }
        }
    }

    private String buildJsonSchema(AgenticTool agenticTool, Method method) {
        Map<String, Property> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter param : method.getParameters()) {
            AgenticToolParam toolParam = param.getAnnotation(AgenticToolParam.class);
            if (toolParam == null) {
                throw new IllegalArgumentException(
                        "All parameters of @AgenticTool method must be annotated with @AgenticToolParam. "
                                + "Missing on parameter '" + param.getName() + "' in method " + method.getName());
            }
            String jsonType = resolveJsonType(param.getType());
            properties.put(toolParam.name(), Property.builder().type(jsonType).description(toolParam.description()).build());
            required.add(toolParam.name());
        }

        Function function = Function.builder()
                .name(agenticTool.name())
                .description(agenticTool.description())
                .parameters(Parameters.builder()
                        .properties(properties)
                        .required(required)
                        .build())
                .build();

        Tool tool = Tool.builder().function(function).build();

        try {
            return OBJECT_MAPPER.writeValueAsString(tool);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize tool JSON schema", e);
        }
    }

    static String resolveJsonType(Class<?> type) {
        if (type == String.class) {
            return "string";
        }
        if (type == Integer.class || type == int.class) {
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
        return null;
    }
}
