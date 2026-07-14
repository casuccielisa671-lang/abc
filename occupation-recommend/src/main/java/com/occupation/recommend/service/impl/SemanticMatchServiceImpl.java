package com.occupation.recommend.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.occupation.common.ai.AiChatClient;
import com.occupation.recommend.service.SemanticMatchService;
import com.occupation.recommend.vo.SemanticMatchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * JD-简历语义匹配实现。
 * <p>
 * 调用 DeepSeek 做 JD 文本与简历文本的语义相似度计算。
 * 结果缓存到 Redis 24 小时，AI 不可用时降级返回空结果。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticMatchServiceImpl implements SemanticMatchService {

    private static final String CACHE_KEY_PREFIX = "semantic:match:";
    private static final long CACHE_TTL_HOURS = 24;

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是一位资深的招聘专家和技术面试官，擅长评估求职者与岗位的匹配度。",
            "请对比以下 JD（职位描述）和简历内容，从语义层面判断两者的匹配程度。",
            "注意：不要做简单的关键词匹配，要理解技能的语义关联。",
            "例如「Spring Boot」和「Java 后端开发」是高度相关的，「Python」和「数据分析」是相关的。",
            "请只返回一个 JSON 对象，不要任何解释性文字、不要 markdown 代码块：",
            "{",
            "  \"similarity\": 整数 0-100，表示整体匹配度",
            "  \"matchedPoints\": [\"匹配点1\", \"匹配点2\"],",
            "  \"gapPoints\": [\"差距点1\", \"差距点2\"]",
            "}",
            "要求：",
            "1. similarity 要有区分度：技能高度匹配 80-100，部分匹配 50-79，弱匹配 20-49，几乎不匹配 0-19",
            "2. matchedPoints 列出 2-5 条具体的匹配点，必须引用 JD 和简历中的具体内容",
            "3. gapPoints 列出 2-5 条具体的差距，说明 JD 要求但简历中缺乏或不足的能力",
            "4. 如果简历和 JD 属于完全不同的领域（如 JD 是医生、简历是程序员），similarity 给 0-10");

    private final AiChatClient aiChatClient;
    private final StringRedisTemplate redisTemplate;

    @Override
    public SemanticMatchVO match(Long userId, Long jobId, String jdText, String resumeText) {
        // 1. 查缓存
        String cacheKey = CACHE_KEY_PREFIX + userId + ":" + jobId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            try {
                SemanticMatchVO vo = JSON.parseObject(cached, SemanticMatchVO.class);
                if (vo != null && vo.getSimilarity() != null) {
                    log.debug("语义匹配命中缓存 userId={}, jobId={}, similarity={}", userId, jobId, vo.getSimilarity());
                    return vo;
                }
            } catch (Exception e) {
                log.warn("语义匹配缓存反序列化失败，将重新计算: {}", e.getMessage());
            }
        }

        // 2. AI 不可用 → 降级
        if (!aiChatClient.isEnabled()) {
            return emptyResult();
        }

        // 3. 调用大模型
        try {
            String userPrompt = "【JD 描述】\n" + jdText + "\n\n【简历内容】\n" + resumeText
                    + "\n\n请按要求返回 JSON。";
            String raw = aiChatClient.askJson(SYSTEM_PROMPT, userPrompt);
            SemanticMatchVO vo = parseResponse(raw);
            if (vo == null) {
                return emptyResult();
            }
            vo.setAiGenerated(true);

            // 4. 写入缓存
            try {
                redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(vo),
                        CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("语义匹配缓存写入失败: {}", e.getMessage());
            }

            return vo;
        } catch (Exception e) {
            log.warn("语义匹配 AI 调用失败，降级: {}", e.getMessage());
            return emptyResult();
        }
    }

    private SemanticMatchVO parseResponse(String raw) {
        try {
            String t = raw.trim();
            if (t.startsWith("```")) {
                int start = t.indexOf('\n');
                int end = t.lastIndexOf("```");
                if (start > 0 && end > start) {
                    t = t.substring(start + 1, end).trim();
                }
            }
            JSONObject json = JSON.parseObject(t);
            SemanticMatchVO vo = new SemanticMatchVO();
            vo.setSimilarity(json.getInteger("similarity"));
            vo.setMatchedPoints(jsonArrayToList(json.getJSONArray("matchedPoints")));
            vo.setGapPoints(jsonArrayToList(json.getJSONArray("gapPoints")));
            if (vo.getSimilarity() == null) {
                return null;
            }
            vo.setSimilarity(Math.max(0, Math.min(100, vo.getSimilarity())));
            return vo;
        } catch (Exception e) {
            log.warn("语义匹配结果 JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private List<String> jsonArrayToList(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return Collections.emptyList();
        }
        return arr.stream().map(Object::toString).collect(Collectors.toList());
    }

    private SemanticMatchVO emptyResult() {
        SemanticMatchVO vo = new SemanticMatchVO();
        vo.setSimilarity(0);
        vo.setMatchedPoints(Collections.emptyList());
        vo.setGapPoints(Collections.emptyList());
        vo.setAiGenerated(false);
        return vo;
    }
}
