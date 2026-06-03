package com.gacfox.proarc.agentic.client;

import com.gacfox.proarc.agentic.client.interceptor.LlmInterceptor;
import com.gacfox.proarc.agentic.exception.LlmException;
import com.gacfox.proarc.agentic.model.openai.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * OpenAI兼容端点大语言模型客户端
 */
public final class OpenAiLlmClient extends AbstractLlmClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Builder
    private OpenAiLlmClient(ModelInfo modelInfo, List<LlmInterceptor> interceptors, HttpClient httpClient) {
        super(modelInfo, interceptors, httpClient);
    }

    @Override
    protected ModelResponse doBlockingChat(ModelRequest modelRequest) {
        try {
            modelRequest.setStream(true);
            return doStreamingChat(modelRequest)
                    .collectList()
                    .filter(chunks -> !chunks.isEmpty())
                    .map(chunks -> {
                        Map<Integer, StringBuilder> contentMap = new HashMap<>();
                        Map<Integer, StringBuilder> reasoningMap = new HashMap<>();
                        Map<Integer, String> roleMap = new HashMap<>();
                        Map<Integer, String> finishReasonMap = new HashMap<>();
                        Map<Integer, Map<Integer, ToolCallBuilder>> toolCallsMap = new TreeMap<>();
                        String id = null;
                        Integer created = null;
                        String model = null;
                        Usage usage = null;

                        for (ModelResponse chunk : chunks) {
                            if (chunk == null) {
                                continue;
                            }
                            if (id == null && chunk.getId() != null) {
                                id = chunk.getId();
                            }
                            if (created == null && chunk.getCreated() != null) {
                                created = chunk.getCreated();
                            }
                            if (model == null && chunk.getModel() != null) {
                                model = chunk.getModel();
                            }
                            if (chunk.getUsage() != null) {
                                usage = chunk.getUsage();
                            }
                            if (chunk.getChoices() == null) {
                                continue;
                            }
                            for (Choice choice : chunk.getChoices()) {
                                int choiceIdx = choice.getIndex() != null ? choice.getIndex() : 0;
                                Delta delta = choice.getDelta();
                                if (delta != null) {
                                    if (delta.getRole() != null) {
                                        roleMap.put(choiceIdx, delta.getRole());
                                    }
                                    if (delta.getContent() != null) {
                                        contentMap.computeIfAbsent(choiceIdx, k -> new StringBuilder()).append(delta.getContent());
                                    }
                                    if (delta.getReasoning() != null) {
                                        reasoningMap.computeIfAbsent(choiceIdx, k -> new StringBuilder()).append(delta.getReasoning());
                                    }
                                    if (delta.getToolCalls() != null) {
                                        Map<Integer, ToolCallBuilder> tcMap = toolCallsMap.computeIfAbsent(choiceIdx, k -> new TreeMap<>());
                                        for (ToolCall tc : delta.getToolCalls()) {
                                            int tcIdx = tc.getIndex() != null ? tc.getIndex() : tcMap.size();
                                            ToolCallBuilder tcb = tcMap.computeIfAbsent(tcIdx, k -> new ToolCallBuilder());
                                            if (tc.getId() != null) {
                                                tcb.id = tc.getId();
                                            }
                                            if (tc.getType() != null) {
                                                tcb.type = tc.getType();
                                            }
                                            if (tc.getFunction() != null) {
                                                if (tc.getFunction().getName() != null) {
                                                    tcb.functionName = tc.getFunction().getName();
                                                }
                                                if (tc.getFunction().getArguments() != null) {
                                                    tcb.argumentsBuilder.append(tc.getFunction().getArguments());
                                                }
                                            }
                                        }
                                    }
                                }
                                if (choice.getFinishReason() != null) {
                                    finishReasonMap.put(choiceIdx, choice.getFinishReason());
                                }
                            }
                        }

                        Set<Integer> allChoiceIndices = new LinkedHashSet<>();
                        allChoiceIndices.addAll(contentMap.keySet());
                        allChoiceIndices.addAll(reasoningMap.keySet());
                        allChoiceIndices.addAll(toolCallsMap.keySet());

                        List<Choice> mergedChoices = new ArrayList<>();
                        for (Integer choiceIdx : allChoiceIndices) {
                            List<ToolCall> mergedToolCalls = null;
                            Map<Integer, ToolCallBuilder> tcMap = toolCallsMap.get(choiceIdx);
                            if (tcMap != null && !tcMap.isEmpty()) {
                                mergedToolCalls = new ArrayList<>();
                                for (Map.Entry<Integer, ToolCallBuilder> entry : tcMap.entrySet()) {
                                    ToolCallBuilder tcb = entry.getValue();
                                    mergedToolCalls.add(ToolCall.builder()
                                            .id(tcb.id)
                                            .index(entry.getKey())
                                            .type(tcb.type)
                                            .function(ToolCallFunction.builder()
                                                    .name(tcb.functionName)
                                                    .arguments(tcb.argumentsBuilder.toString())
                                                    .build())
                                            .build());
                                }
                            }
                            Message message = Message.builder()
                                    .role(roleMap.getOrDefault(choiceIdx, Message.ROLE_ASSISTANT))
                                    .content(contentMap.containsKey(choiceIdx) ? contentMap.get(choiceIdx).toString() : null)
                                    .reasoningContent(reasoningMap.containsKey(choiceIdx) ? reasoningMap.get(choiceIdx).toString() : null)
                                    .toolCalls(mergedToolCalls)
                                    .build();
                            mergedChoices.add(Choice.builder()
                                    .index(choiceIdx)
                                    .message(message)
                                    .finishReason(finishReasonMap.get(choiceIdx))
                                    .build());
                        }

                        return ModelResponse.builder()
                                .id(id)
                                .created(created)
                                .model(model)
                                .object("chat.completion")
                                .choices(mergedChoices)
                                .usage(usage)
                                .build();
                    })
                    .block();
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    private static class ToolCallBuilder {
        String id;
        String type;
        String functionName;
        final StringBuilder argumentsBuilder = new StringBuilder();
    }

    @Override
    protected Flux<ModelResponse> doStreamingChat(ModelRequest modelRequest) {
        modelRequest.setStream(true);

        return webClient.post()
                .uri(modelInfo.getEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + modelInfo.getSk())
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .header(HttpHeaders.TRANSFER_ENCODING, "chunked")
                .bodyValue(modelRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(data -> data != null && !"[DONE]".equals(data.trim()))
                .flatMap(data -> {
                    try {
                        return Mono.just(OBJECT_MAPPER.readValue(data, ModelResponse.class));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Failed to parse streaming response chunk", e));
                    }
                })
                .onErrorMap(e -> e instanceof LlmException ? e : mapException(e));
    }
}
