package com.occupation.common.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 大模型对话客户端 — OpenAI 兼容 {@code /chat/completions}
 * <p>
 * 全平台唯一的 LLM 出口：报告摘要、简历诊断、职业顾问、匹配理由都走这里。
 * <p>
 * <b>失败语义</b>：一律抛 {@link AiUnavailableException}，调用方负责降级。
 * 本类不做降级 —— 降级文案是业务语义，属于各自的 Service。
 * <p>
 * <b>重试</b>：只对超时和 5xx 重试。4xx 是请求本身有问题（密钥错、模型名错、超长），
 * 重试只会重复挨打，直接抛。
 *
 * @author occupation-team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatClient {

    private final AiProperties props;

    private RestTemplate restTemplate;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getReadTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
        // 默认的 StringHttpMessageConverter 用 ISO-8859-1 读响应体，中文会变成 ????
        this.restTemplate.getMessageConverters().add(0,
                new org.springframework.http.converter.StringHttpMessageConverter(StandardCharsets.UTF_8));

        if (props.usable()) {
            log.info("AI 已启用: model={}, baseUrl={}", props.getModel(), props.getBaseUrl());
        } else {
            log.info("AI 未启用（app.ai.enabled=false 或未配置 api-key），相关能力将降级为规则化输出");
        }
    }

    /** 密钥与开关是否就绪。调用方可据此跳过 prompt 拼装，省一次异常 */
    public boolean isEnabled() {
        return props.usable();
    }

    /**
     * 单轮问答
     *
     * @param systemPrompt 角色设定，决定回答的身份、口径与格式，可为 null
     * @param userPrompt   用户输入
     */
    public String ask(String systemPrompt, String userPrompt) {
        List<AiMessage> messages = new ArrayList<>(2);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(AiMessage.system(systemPrompt));
        }
        messages.add(AiMessage.user(userPrompt));
        return chat(messages, false, null);
    }

    /**
     * 要求模型返回严格 JSON（DeepSeek 的 json_object 模式）。
     * <p>
     * 用于简历诊断这类需要结构化结果的场景 —— 比让模型输出 Markdown 再正则去抠稳得多。
     * <b>提示词里必须出现 "json" 字样</b>，否则 DeepSeek 会拒绝该模式。
     */
    public String askJson(String systemPrompt, String userPrompt) {
        List<AiMessage> messages = new ArrayList<>(2);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(AiMessage.system(systemPrompt));
        }
        messages.add(AiMessage.user(userPrompt));
        return chat(messages, true, null);
    }

    /**
     * 多轮对话（职业顾问用）
     *
     * @param messages 完整消息列表，含 system 与历史轮次
     */
    public String chat(List<AiMessage> messages) {
        return chat(messages, false, null);
    }

    /**
     * 多轮对话并覆盖采样温度。
     * <p>
     * 全局温度按报告/诊断类调低（要稳定可复现），对话类需要更自然的措辞，按需调高。
     */
    public String chat(List<AiMessage> messages, double temperature) {
        return chat(messages, false, temperature);
    }

    private String chat(List<AiMessage> messages, boolean jsonMode, Double temperature) {
        if (!props.usable()) {
            throw new AiUnavailableException("AI 未启用或未配置 api-key");
        }
        String body = buildRequestBody(messages, jsonMode, temperature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey().trim());

        String url = trimTrailingSlash(props.getBaseUrl()) + "/chat/completions";
        int attempts = props.getMaxRetries() + 1;
        RuntimeException last = null;

        for (int i = 1; i <= attempts; i++) {
            long start = System.currentTimeMillis();
            try {
                String response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
                return parseContent(response, System.currentTimeMillis() - start);

            } catch (HttpClientErrorException e) {
                // 4xx：密钥无效 / 模型名错 / 上下文超长 —— 重试无意义
                throw new AiUnavailableException(
                        "AI 请求被拒绝(" + e.getRawStatusCode() + "): " + shorten(e.getResponseBodyAsString()), e);

            } catch (HttpStatusCodeException e) {
                last = new AiUnavailableException(
                        "AI 上游错误(" + e.getRawStatusCode() + ")", e);
                log.warn("AI 调用失败(第 {}/{} 次), status={}", i, attempts, e.getRawStatusCode());

            } catch (ResourceAccessException e) {
                last = new AiUnavailableException("AI 请求超时或网络不可达", e);
                log.warn("AI 调用超时(第 {}/{} 次): {}", i, attempts, e.getMessage());

            } catch (Exception e) {
                last = new AiUnavailableException("AI 调用异常: " + e.getMessage(), e);
                log.warn("AI 调用异常(第 {}/{} 次)", i, attempts, e);
            }
        }
        throw last;
    }

    private String buildRequestBody(List<AiMessage> messages, boolean jsonMode, Double temperature) {
        JSONArray arr = new JSONArray();
        for (AiMessage m : messages) {
            JSONObject o = new JSONObject();
            o.put("role", m.getRole());
            o.put("content", m.getContent());
            arr.add(o);
        }
        JSONObject body = new JSONObject();
        body.put("model", props.getModel());
        body.put("messages", arr);
        body.put("temperature", temperature == null ? props.getTemperature() : temperature);
        body.put("max_tokens", props.getMaxTokens());
        if (jsonMode) {
            JSONObject fmt = new JSONObject();
            fmt.put("type", "json_object");
            body.put("response_format", fmt);
        }
        return body.toJSONString();
    }

    /** 取 choices[0].message.content，顺便把 token 用量打进日志便于估算成本 */
    private String parseContent(String response, long costMs) {
        if (response == null || response.isEmpty()) {
            throw new AiUnavailableException("AI 返回空响应");
        }
        JSONObject json = JSON.parseObject(response);
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new AiUnavailableException("AI 返回缺少 choices: " + shorten(response));
        }
        String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
        if (content == null || content.trim().isEmpty()) {
            throw new AiUnavailableException("AI 返回内容为空");
        }
        JSONObject usage = json.getJSONObject("usage");
        log.info("AI 调用成功: 耗时={}ms, prompt_tokens={}, completion_tokens={}", costMs,
                usage == null ? "?" : usage.getInteger("prompt_tokens"),
                usage == null ? "?" : usage.getInteger("completion_tokens"));
        return content.trim();
    }

    private static String trimTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String shorten(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
