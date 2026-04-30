package com.gacfox.proarc.agentic.model.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 标准（OpenAI规范）大模型响应
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelResponse implements Serializable {
    /**
     * 消息ID
     */
    @JsonProperty("id")
    private String id;
    /**
     * 创建时间戳
     */
    @JsonProperty("created")
    private Integer created;
    /**
     * 模型名称
     */
    @JsonProperty("model")
    private String model;
    /**
     * 对象类型（固定值chat.completion）
     */
    @JsonProperty("object")
    private String object;
    /**
     * 回复列表
     */
    @JsonProperty("choices")
    private List<Choice> choices;
    /**
     * tokens使用情况
     */
    @JsonProperty("usage")
    private Usage usage;

    /**
     * 获取第1个choice的回复文本，仅阻塞调用时有效
     *
     * @return 回复文本，无内容时返回null
     */
    public String extractBlockingContent() {
        Message message = getFirstChoiceMessage();
        return message != null && message.getContent() != null ? message.getContent().toString() : null;
    }

    /**
     * 获取第1个choice的Reasoning内容，仅阻塞调用时有效
     *
     * @return Reasoning内容，无Reasoning时返回null
     */
    public String extractBlockingReasoningContent() {
        Message message = getFirstChoiceMessage();
        return message != null ? message.getReasoningContent() : null;
    }

    /**
     * 获取第1个choice的工具调用列表，仅阻塞调用时有效
     *
     * @return 工具调用列表，无工具调用时返回空列表
     */
    public List<ToolCall> extractBlockingToolCalls() {
        Message message = getFirstChoiceMessage();
        return message != null && message.getToolCalls() != null ? message.getToolCalls() : Collections.emptyList();
    }

    /**
     * 获取第1个choice的停止原因，仅阻塞调用时有效
     *
     * @return 停止原因，如 stop、tool_calls、length
     */
    public String extractBlockingFinishReason() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.getFirst().getFinishReason();
    }

    private Message getFirstChoiceMessage() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.getFirst().getMessage();
    }
}
