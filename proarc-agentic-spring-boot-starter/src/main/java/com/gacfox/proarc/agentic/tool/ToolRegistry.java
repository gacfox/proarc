package com.gacfox.proarc.agentic.tool;

import com.gacfox.proarc.agentic.schema.AgenticSchemaBuilder;
import com.gacfox.proarc.agentic.model.openai.Tool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体工具注册中心
 */
@Slf4j
public class ToolRegistry {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final AgenticSchemaBuilder schemaBuilder = new AgenticSchemaBuilder();
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

            validateToolMethod(method);
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

    private void validateToolMethod(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters.length > 1) {
            throw new IllegalArgumentException("@AgenticTool method must declare zero or one DTO parameter: "
                    + method.getDeclaringClass().getName() + "." + method.getName());
        }
        if (parameters.length == 1 && parameters[0].getAnnotation(AgenticToolParam.class) == null) {
            throw new IllegalArgumentException("@AgenticTool DTO parameter must be annotated with @AgenticToolParam: "
                    + method.getDeclaringClass().getName() + "." + method.getName());
        }
    }

    private String buildJsonSchema(AgenticTool agenticTool, Method method) {
        Class<?> dtoType = method.getParameterCount() == 1 ? method.getParameterTypes()[0] : null;
        Tool tool = schemaBuilder.buildTool(agenticTool.name(), agenticTool.description(), dtoType);

        try {
            return OBJECT_MAPPER.writeValueAsString(tool);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize tool JSON schema", e);
        }
    }
}
