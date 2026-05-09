package com.gacfox.proarc.agentic.agent;

import com.gacfox.proarc.agentic.agent.interceptor.AgentInterceptor;
import com.gacfox.proarc.agentic.agent.interceptor.AgentInterceptorChain;
import com.gacfox.proarc.agentic.client.LlmClient;
import com.gacfox.proarc.agentic.model.ChatRequest;
import com.gacfox.proarc.agentic.model.openai.*;
import com.gacfox.proarc.agentic.tool.AgenticToolParam;
import com.gacfox.proarc.agentic.tool.ToolDefinition;
import com.gacfox.proarc.agentic.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.*;

/**
 * ReAct智能体执行器
 */
@Slf4j
@Builder
public class ReActAgentExecutor {
    private static final String FINAL_ANSWER_TOOL = "final_answer";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LlmClient defaultLlmClient;
    private final ToolRegistry toolRegistry;
    private final List<String> defaultToolNames;
    private final List<AgentInterceptor> interceptors;
    @Builder.Default
    private final int maxIterations = 50;

    public Flux<AgentResponse> execute(AgentContext context) {
        return Flux.<AgentResponse>create(sink -> {
            try {
                doExecute(context, sink);
            } catch (Exception e) {
                log.error("Agent execution error", e);
                sink.next(AgentResponse.error(e.getMessage()));
            } finally {
                sink.complete();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void doExecute(AgentContext context, FluxSink<AgentResponse> sink) {
        context.setMessages(new ArrayList<>(context.getMessages()));
        List<AgentInterceptor> sortedInterceptors = Optional.ofNullable(interceptors)
                .orElseGet(Collections::emptyList)
                .stream()
                .sorted(Comparator.comparingInt(AgentInterceptor::getOrder))
                .toList();

        for (int i = 0; i < this.maxIterations; i++) {
            AgentInterceptorChain chain = new AgentInterceptorChain() {
                int index = 0;

                @Override
                public AgentLoopResult next(AgentContext context) {
                    if (index < sortedInterceptors.size()) {
                        AgentInterceptor interceptor = sortedInterceptors.get(index++);
                        return interceptor.intercept(context, this);
                    }
                    return executeLoop(context);
                }
            };

            AgentLoopResult loopResult = chain.next(context);
            for (AgentResponse response : loopResult.getResponses()) {
                sink.next(response);
            }
            if (loopResult.isFinished() || loopResult.isSuspended()) {
                return;
            }
        }

        sink.next(AgentResponse.error("Agent reached maximum iterations"));
    }

    private AgentLoopResult executeLoop(AgentContext context) {
        LlmClient llmClient = context.getLlmClient() != null ? context.getLlmClient() : defaultLlmClient;
        List<String> toolNames = context.getToolNames() != null && !context.getToolNames().isEmpty()
                ? context.getToolNames() : defaultToolNames;
        List<ToolDefinition> toolDefs = resolveToolDefinitions(toolNames);
        List<Tool> tools = buildToolList(toolDefs);

        Map<String, ToolDefinition> toolMap = new HashMap<>();
        for (ToolDefinition td : toolDefs) {
            toolMap.put(td.getToolName(), td);
        }

        List<AgentResponse> responses = new ArrayList<>();
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(context.getMessages())
                .tools(tools)
                .build();
        ModelResponse response = llmClient.blockingChat(chatRequest);

        Message assistantMessage = response.getChoices().getFirst().getMessage();
        context.getMessages().add(assistantMessage);

        String thinking = response.extractBlockingReasoningContent();
        if (StringUtils.hasText(thinking)) {
            responses.add(AgentResponse.thinking(thinking));
        }

        String finishReason = response.extractBlockingFinishReason();
        if (!"tool_calls".equals(finishReason)) {
            context.getMessages().add(Message.builder()
                    .role(Message.ROLE_USER)
                    .content("""
                            No tool call was detected.
                            To finish: call final_answer.
                            To continue working: call the required tool immediately.
                            Plain text descriptions of tool usage are not allowed.
                            """)
                    .build());
            return AgentLoopResult.continueWith(responses);
        }

        List<ToolCall> toolCalls = response.extractBlockingToolCalls();
        for (ToolCall toolCall : toolCalls) {
            ToolCallFunction fn = toolCall.getFunction();
            String toolName = fn.getName();
            String arguments = fn.getArguments();

            responses.add(AgentResponse.toolCall(toolCall.getId(), toolName, arguments));

            if (FINAL_ANSWER_TOOL.equals(toolName)) {
                String finalMessage = extractFinalAnswer(arguments);
                responses.add(AgentResponse.finalAnswer(finalMessage));
                return AgentLoopResult.finishWith(responses);
            }

            String result = invokeTool(toolMap, toolName, arguments);
            responses.add(AgentResponse.toolResult(toolCall.getId(), toolName, result));

            context.getMessages().add(Message.builder()
                    .role(Message.ROLE_TOOL)
                    .toolCallId(toolCall.getId())
                    .content(result)
                    .build());
        }
        return AgentLoopResult.continueWith(responses);
    }

    private List<ToolDefinition> resolveToolDefinitions(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }
        List<ToolDefinition> toolDefs = new ArrayList<>(toolNames.size());
        for (String toolName : toolNames) {
            ToolDefinition toolDef = toolRegistry.getAgenticTool(toolName);
            if (toolDef == null) {
                throw new IllegalStateException("Tool not found in registry: '" + toolName + "'");
            }
            toolDefs.add(toolDef);
        }
        return toolDefs;
    }

    private List<Tool> buildToolList(List<ToolDefinition> toolDefs) {
        List<Tool> tools = new ArrayList<>();
        for (ToolDefinition td : toolDefs) {
            try {
                tools.add(OBJECT_MAPPER.readValue(td.getJsonSchema(), Tool.class));
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse tool JSON schema: " + td.getToolName(), e);
            }
        }
        tools.add(Tool.builder().function(
                Function.builder()
                        .name(FINAL_ANSWER_TOOL)
                        .description("Submit the final answer to the user's request and end the agent loop")
                        .parameters(Parameters.builder()
                                .properties(Map.of("message",
                                        Property.builder()
                                                .type("string")
                                                .description("The final answer message")
                                                .build()))
                                .required(List.of("message"))
                                .build())
                        .build()).build());
        return tools;
    }

    private String invokeTool(Map<String, ToolDefinition> toolMap, String toolName, String arguments) {
        ToolDefinition toolDef = toolMap.get(toolName);
        if (toolDef == null) {
            return "Error: tool '" + toolName + "' not found";
        }
        try {
            JsonNode argsNode = OBJECT_MAPPER.readTree(arguments);
            java.lang.reflect.Method method = toolDef.getMethod();
            java.lang.reflect.Parameter[] params = method.getParameters();
            Object[] args = new Object[params.length];
            for (int j = 0; j < params.length; j++) {
                AgenticToolParam paramAnn =
                        params[j].getAnnotation(AgenticToolParam.class);
                String paramName = paramAnn != null ? paramAnn.name() : params[j].getName();
                JsonNode valueNode = argsNode.get(paramName);
                if (valueNode != null && !valueNode.isNull()) {
                    args[j] = OBJECT_MAPPER.convertValue(valueNode, params[j].getType());
                } else {
                    args[j] = null;
                }
            }
            Object result = method.invoke(toolDef.getBeanInstance(), args);
            return result != null ? result.toString() : "null";
        } catch (Exception e) {
            log.error("Tool invocation error: {}", toolName, e);
            return "Error: " + e.getMessage();
        }
    }

    private String extractFinalAnswer(String arguments) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(arguments);
            JsonNode msgNode = node.get("message");
            return msgNode != null ? msgNode.asText() : arguments;
        } catch (Exception e) {
            return arguments;
        }
    }
}
