package com.gacfox.proarc.agentic.client;

import com.gacfox.proarc.agentic.exception.LlmException;
import com.gacfox.proarc.agentic.client.interceptor.LlmInterceptor;
import com.gacfox.proarc.agentic.model.openai.ModelInfo;
import com.gacfox.proarc.agentic.model.openai.ModelRequest;
import com.gacfox.proarc.agentic.model.openai.ModelResponse;
import lombok.Builder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.util.List;

/**
 * OpenAI兼容端点大语言模型客户端
 */
public class OpenAiLlmClient extends AbstractLlmClient {
    @Builder
    private OpenAiLlmClient(ModelInfo modelInfo, List<LlmInterceptor> interceptors, HttpClient httpClient) {
        super(modelInfo, interceptors, httpClient);
    }

    @Override
    protected ModelResponse doBlockingChat(ModelRequest modelRequest) {
        try {
            return webClient.post()
                    .uri(modelInfo.getEndpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + modelInfo.getSk())
                    .header(HttpHeaders.TRANSFER_ENCODING, "chunked")
                    .bodyValue(modelRequest)
                    .retrieve()
                    .bodyToMono(ModelResponse.class)
                    .block();
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    @Override
    protected Flux<ModelResponse> doStreamingChat(ModelRequest modelRequest) {
        return webClient.post()
                .uri(modelInfo.getEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + modelInfo.getSk())
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .header(HttpHeaders.TRANSFER_ENCODING, "chunked")
                .bodyValue(modelRequest)
                .retrieve()
                .bodyToFlux(ModelResponse.class)
                .onErrorMap(e -> e instanceof LlmException ? e : mapException(e));
    }
}
