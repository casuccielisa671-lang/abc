package com.occupation.analysis.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.analysis.entity.JobDetail;
import com.occupation.analysis.mapper.JobDetailMapper;
import com.occupation.analysis.service.DataCleanService;
import com.occupation.common.entity.RawJobData;
import com.occupation.common.mapper.RawJobDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据清洗服务实现
 * <p>
 * 数据流：Kafka(raw-job-data) → JobDataCleanListener → 本服务 → job_detail 表。
 * 同时提供存量补偿：定时扫描 raw_job_data 中 status=RAW 的记录重新清洗。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCleanServiceImpl implements DataCleanService {

    private final JobDetailMapper jobDetailMapper;
    private final RawJobDataMapper rawJobDataMapper;

    @Override
    public boolean cleanAndSave(String rawContent, String source, String sourceUrl) {
        // 1. 去重：source_url 已存在则跳过
        Long exists = jobDetailMapper.selectCount(
                new LambdaQueryWrapper<JobDetail>().eq(JobDetail::getSourceUrl, sourceUrl));
        if (exists != null && exists > 0) {
            log.debug("清洗跳过（重复）: {}", sourceUrl);
            return false;
        }

        // 2. 解析原始 JSON
        JSONObject json;
        try {
            json = JSON.parseObject(rawContent);
        } catch (Exception e) {
            log.warn("清洗失败（非法 JSON）: {}", sourceUrl);
            return false;
        }

        // 3. 必填字段校验：无标题/无公司 视为非法数据
        String title = json.getString("title");
        String company = json.getString("company");
        if (title == null || title.isEmpty() || company == null || company.isEmpty()) {
            log.warn("清洗丢弃（缺少必填字段）: {}", sourceUrl);
            return false;
        }

        // 4. 字段标准化
        JobDetail job = new JobDetail();
        job.setTitle(title);
        job.setCompany(company);
        job.setCity(normalizeCity(json.getString("city")));
        job.setIndustry(json.getString("industry"));
        job.setSalaryMin(normalizeSalary(json.getInteger("salaryMin")));
        job.setSalaryMax(normalizeSalary(json.getInteger("salaryMax")));
        job.setEducation(normalizeEducation(json.getString("education")));
        job.setExperience(json.getString("experience"));
        job.setSkills(extractSkills(json));
        job.setDescription(json.getString("description"));
        job.setPublishDate(parseDate(json.getString("publishDate")));
        job.setSource(source);
        job.setSourceUrl(sourceUrl);
        job.setCreateTime(LocalDateTime.now());

        jobDetailMapper.insert(job);
        log.info("清洗入库: title={}, company={}, city={}", title, company, job.getCity());
        return true;
    }

    @Override
    public int cleanPendingRawData() {
        // 每批最多处理 500 条，避免单次任务过长
        List<RawJobData> pending = rawJobDataMapper.selectList(
                new LambdaQueryWrapper<RawJobData>()
                        .eq(RawJobData::getStatus, "RAW")
                        .last("LIMIT 500"));

        int success = 0;
        for (RawJobData raw : pending) {
            boolean ok = cleanAndSave(raw.getRawContent(), raw.getSource(), raw.getSourceUrl());
            // 无论成功/重复，都标记为已清洗，避免反复扫描
            raw.setStatus("CLEANED");
            rawJobDataMapper.updateById(raw);
            if (ok) {
                success++;
            }
        }
        log.info("存量清洗完成: 扫描 {} 条，入库 {} 条", pending.size(), success);
        return success;
    }

    // ==================== 标准化规则 ====================

    /** 城市标准化："北京市" → "北京"；空值归为"其他" */
    private String normalizeCity(String city) {
        if (city == null || city.isEmpty()) {
            return "其他";
        }
        return city.replaceAll("[市省]$", "");
    }

    /** 薪资标准化：负数/异常值归 null；单位统一为 元/月（如遇 "15k" 类文本格式在爬虫解析层转换） */
    private Integer normalizeSalary(Integer salary) {
        if (salary == null || salary < 0) {
            return null;
        }
        return salary;
    }

    /** 学历映射：统一为 专科/本科/硕士/博士/不限 五档 */
    private String normalizeEducation(String education) {
        if (education == null || education.isEmpty()) {
            return "不限";
        }
        if (education.contains("专")) return "专科";
        if (education.contains("本")) return "本科";
        if (education.contains("硕") || education.contains("研究生")) return "硕士";
        if (education.contains("博")) return "博士";
        return "不限";
    }

    /** 技能提取：优先取 skills 数组；缺失时返回空数组
     *  TODO(P2): 从 description 中按技能词库（Java/Python/MySQL...）关键词匹配补全 */
    private String extractSkills(JSONObject json) {
        JSONArray skills = json.getJSONArray("skills");
        return skills != null ? skills.toJSONString() : "[]";
    }

    /** 发布日期解析：解析失败时取当天 */
    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
