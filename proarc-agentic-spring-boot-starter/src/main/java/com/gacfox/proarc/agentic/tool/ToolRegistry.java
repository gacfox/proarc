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
     * 通过扫描 {@link AgenticTool} 注解注册工具
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

            validateToolMethod(method);
            String jsonSchema = buildJsonSchema(agenticTool, method);
            method.setAccessible(true);
            ToolDefinition toolDef = new ToolDefinition(agenticTool.name(), agenticTool.description(), jsonSchema, buildReflectionInvoker(bean, method));
            register(toolDef);
            log.info("Registered agentic tool from bean: {} -> {}.{}", agenticTool.name(), clazz.getSimpleName(), method.getName());
        }
    }

    /**
     * 注册工具定义，工具名全局唯一
     *
     * @param toolDef 工具定义
     */
    public void register(ToolDefinition toolDef) {
        String toolName = toolDef.getToolName();
        if (registry.putIfAbsent(toolName, toolDef) != null) {
            throw new IllegalStateException(
                    "Duplicate agentic tool name detected: '" + toolName + "'. Tool names must be globally unique.");
        }
    }

    /**
     * 取消注册工具
     *
     * @param toolName 工具名
     */
    public void unregister(String toolName) {
        if (registry.remove(toolName) != null) {
            log.info("Unregistered agentic tool: {}", toolName);
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

    private ToolInvoker buildReflectionInvoker(Object bean, Method method) {
        return arguments -> {
            Object result;
            if (method.getParameterCount() == 0) {
                result = method.invoke(bean);
            } else {
                Object arg = OBJECT_MAPPER.readValue(arguments, method.getParameterTypes()[0]);
                result = method.invoke(bean, arg);
            }
            return result != null ? result.toString() : "null";
        };
    }
}
