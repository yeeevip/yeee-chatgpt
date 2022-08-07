package vip.yeee.app.chatgpt.client.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vip.yeee.app.chatgpt.client.domain.redis.ChatRedisRepository;
import vip.yeee.app.chatgpt.client.model.ChatParams;
import vip.yeee.app.chatgpt.client.properties.OpenaiApiProperties;
import vip.yeee.memo.base.model.exception.BizException;
import vip.yeee.memo.common.httpclient.okhttp.kit.OkHttp3Kit;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class OpenaiApiService {

    @Autowired
    private OkHttpClient okHttpClient;
    @Autowired
    private OkHttp3Kit okHttp3Kit;
    @Resource
    private OpenaiApiProperties openaiApiProperties;
    @Resource
    private ChatRedisRepository chatRedisRepository;

    public EventSource chatCompletions(Map<String, String> headers, ChatParams params, EventSourceListener eventSourceListener) {
        try {
            EventSource.Factory factory = EventSources.createFactory(okHttpClient);
            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(params);
            Request request = new Request.Builder()
                    .url(getApiHost() + "/v1/chat/completions")
                    .post(RequestBody.create(MediaType.parse(ContentType.JSON.getValue()), requestBody))
                    .headers(Headers.of(headers))
                    .build();
            log.info("【CHAT-GPT】- 请求 = {}", request);
            return factory.newEventSource(request, eventSourceListener);
        } catch (Exception e) {
            log.error("【CHAT-GPT】- 请求出错", e);
            throw new BizException("chatCompletions error");
        }
    }

    public String queryBillingCreditGrants(Map<String, String> headers) {
        String url = getApiHost() + "/v1/dashboard/billing/subscription";
        return okHttp3Kit.get(url, headers);
    }

    public String queryBillingUsed(String st, String ed, Map<String, String> headers) {
        String url = getApiHost() + String.format("/v1/dashboard/billing/usage?start_date=%s&end_date=%s", st, ed);
        return okHttp3Kit.get(url, headers);
    }

    public String getApiHost() {
        return RandomUtil.randomEle(Arrays.asList(Optional.ofNullable(chatRedisRepository.getApiHost())
                .orElse(openaiApiProperties.getChat().getHost()).split(",")));
    }
}
