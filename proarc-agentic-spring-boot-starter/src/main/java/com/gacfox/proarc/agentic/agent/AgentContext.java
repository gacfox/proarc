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
     * 自定义变量
     */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();
}
