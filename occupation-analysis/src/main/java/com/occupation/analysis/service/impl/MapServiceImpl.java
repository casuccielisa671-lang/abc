package com.occupation.analysis.service.impl;

import cn.hutool.core.util.StrUtil;
import com.occupation.analysis.mapper.MapJobMapper;
import com.occupation.analysis.service.MapService;
import com.occupation.analysis.vo.CityStatVO;
import com.occupation.analysis.vo.JobCityHeatVO;
import com.occupation.analysis.vo.RecommendJobVO;
import com.occupation.common.exception.BizException;
import com.occupation.common.util.CityGeoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 职业城市聚集度 — 基于 job_detail 实时聚合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private static final int RECOMMEND_LIMIT = 24;

    private final MapJobMapper mapJobMapper;

    @Override
    public List<RecommendJobVO> recommendJobs() {
        List<Map<String, Object>> rows = mapJobMapper.selectRecommendJobs(RECOMMEND_LIMIT);
        List<RecommendJobVO> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            RecommendJobVO vo = new RecommendJobVO();
            vo.setJobName(String.valueOf(row.get("jobName")));
            Object cnt = row.get("jobCount");
            vo.setJobCount(cnt instanceof Number ? ((Number) cnt).longValue() : 0L);
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<JobCityHeatVO> getJobCityHeat(String jobName) {
        if (StrUtil.isBlank(jobName)) {
            throw new BizException(400, "职业名称不能为空");
        }
        String trimmed = jobName.trim();
        List<Map<String, Object>> rows = mapJobMapper.selectCityGatherByJob(trimmed);
        if (rows.isEmpty()) {
            rows = mapJobMapper.selectCityGatherByJobLike(trimmed);
        }
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }

        long maxCnt = 1L;
        for (Map<String, Object> row : rows) {
            long c = toLong(row.get("gatherCnt"));
            if (c > maxCnt) {
                maxCnt = c;
            }
        }

        List<JobCityHeatVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String city = String.valueOf(row.get("cityName"));
            Optional<double[]> coord = CityGeoUtil.resolve(city);
            if (!coord.isPresent()) {
                continue;
            }
            long cnt = toLong(row.get("gatherCnt"));
            JobCityHeatVO vo = new JobCityHeatVO();
            vo.setCityName(city);
            vo.setLongitude(coord.get()[0]);
            vo.setLatitude(coord.get()[1]);
            vo.setGatherValue(BigDecimal.valueOf(cnt)
                    .divide(BigDecimal.valueOf(maxCnt), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<CityStatVO> cityDistribution() {
        List<Map<String, Object>> rows = mapJobMapper.selectCityDistribution();
        List<CityStatVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String city = String.valueOf(row.get("cityName"));
            Optional<double[]> coord = CityGeoUtil.resolve(city);
            if (!coord.isPresent()) {
                continue;
            }
            CityStatVO vo = new CityStatVO();
            vo.setCityName(city);
            vo.setLongitude(coord.get()[0]);
            vo.setLatitude(coord.get()[1]);
            vo.setJobCount((int) toLong(row.get("jobCount")));
            vo.setAvgSalary((int) toLong(row.get("avgSalary")));
            result.add(vo);
        }
        return result;
    }

    private long toLong(Object v) {
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return 0L;
    }
}
