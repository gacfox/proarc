package com.gacfox.proarc.agentic.tool;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * 智能体工具定义
 */
@Getter
@AllArgsConstructor
public class ToolDefinition implements Serializable {
    /**
     * 工具名
     */
    private final String toolName;
    /**
     * 工具Bean实例
     */
    private transient Object beanInstance;
    /**
     * 工具Java方法
     */
    private final Method method;
    /**
     * 工具的OpenAI JSON Schema
     */
    private final String jsonSchema;
}
