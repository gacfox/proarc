package com.gacfox.proarc.agentic.agent;

import com.gacfox.proarc.agentic.client.LlmClient;
import com.gacfox.proarc.agentic.model.openai.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体执行上下文
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentContext implements Serializable {
    /**
     * 会话上下文ID
     */
    private String contextId;
    /**
     * 消息列表
     */
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    /**
     * LLM客户端
     */
    private LlmClient llmClient;
    /**
     * 工具名称列表
     */
    private List<String> toolNames;
    /**
     * 温度
     */
    @Builder.Default
    private Double temperature = 0.7;
    /**
     * 是否开启思考
     */
    private Boolean enableThinking;
    /**
     * 核采样参数
     */
    private Double topP;
    /**
     * 前k个最可能token
     */
    private Integer topK;
    /**
     * 惩罚新出现的token
     */
    @Builder.Default
    private Double presencePenalty = 0.0;
    /**
     * 不惩罚重复出现的token
     */
    @Builder.Default
    private Double frequencyPenalty = 0.0;
    /**
     * 随机数种子
     */
    private Integer seed;
    /**
     * 控制生成文本长度
     */
    private Integer maxTokens;
    /**
     * 自定义变量
     */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 创建当前上下文的防御性快照。
     *
     * <p>容器类（messages / toolNames / variables）做浅拷贝——防止工具增删条目污染主循环；
     * 元素对象本身共享引用，工具不应修改元素内容（违反者属工具实现 bug）。
     * <p>LlmClient 等复杂资源对象引用共享——不可重拷，且工具调用 LLM 是合理用例。
     */
    public AgentContext snapshot() {
        return AgentContext.builder()
                .contextId(contextId)
                .messages(messages == null ? null : new ArrayList<>(messages))
                .llmClient(llmClient)
                .toolNames(toolNames == null ? null : new ArrayList<>(toolNames))
                .temperature(temperature)
                .enableThinking(enableThinking)
                .topP(topP)
                .topK(topK)
                .presencePenalty(presencePenalty)
                .frequencyPenalty(frequencyPenalty)
                .seed(seed)
                .maxTokens(maxTokens)
                .variables(variables == null ? null : new HashMap<>(variables))
                .build();
    }
}
