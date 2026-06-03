package com.gacfox.proarc.agentic.model.openai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 模型配置信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModelInfo implements Serializable {
    /**
     * 提供商：openai OpenAI兼容API
     */
    private String provider;
    /**
     * 模型名
     */
    private String model;
    /**
     * OpenAI兼容API端点（/v1/chat/completions）
     */
    private String endpoint;
    /**
     * API密钥
     */
    private String sk;
    /**
     * 模型上下文
     */
    private Integer maxTokens;
    /**
     * 大语言模型额外能力列表：reasoning 支持思考，tool 支持工具调用，vision 支持图片输入
     */
    private List<String> capabilities;

    /**
     * 支持思考
     */
    public static final String CAPABILITY_REASONING = "reasoning";
    /**
     * 支持工具调用
     */
    public static final String CAPABILITY_TOOL = "tool";
    /**
     * 支持图片输入
     */
    public static final String CAPABILITY_VISION = "vision";
}
