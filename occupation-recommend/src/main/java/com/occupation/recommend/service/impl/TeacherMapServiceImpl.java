package com.occupation.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.auth.service.TeacherScopeService;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.util.CityGeoUtil;
import com.occupation.recommend.entity.JobApplication;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.mapper.JobApplicationMapper;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.service.TeacherMapService;
import com.occupation.recommend.vo.MapCityCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师端地图数据实现。可见范围解析复用 {@link TeacherScopeService}，坐标复用 {@link CityGeoUtil}。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherMapServiceImpl implements TeacherMapService {

    private final TeacherScopeService scopeService;
    private final StudentProfileService profileService;
    private final JobApplicationMapper applicationMapper;
    private final JobDetailService jobDetailService;

    @Override
    public List<MapCityCountVO> studentIntentCities() {
        Set<Long> ids = visibleIds();
        List<SysStudentProfile> profiles = profileService.listByUserIds(ids);
        Map<String, Integer> byCity = new LinkedHashMap<>();
        for (SysStudentProfile p : profiles) {
            merge(byCity, p.getExpectedCity());
        }
        return toVOs(byCity);
    }

    @Override
    public List<MapCityCountVO> applicationCities() {
        Set<Long> ids = visibleIds();
        List<JobApplication> apps;
        if (ids == null) {
            apps = applicationMapper.selectList(null);
        } else if (ids.isEmpty()) {
            return Collections.emptyList();
        } else {
            apps = applicationMapper.selectList(
                    new LambdaQueryWrapper<JobApplication>().in(JobApplication::getUserId, ids));
        }
        if (apps.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> jobIds = apps.stream().map(JobApplication::getJobId).distinct().collect(Collectors.toList());
        Map<Long, JobDetailVO> jobs = jobDetailService.listByIds(jobIds).stream()
                .collect(Collectors.toMap(JobDetailVO::getId, j -> j, (a, b) -> a));
        Map<String, Integer> byCity = new LinkedHashMap<>();
        for (JobApplication a : apps) {
            JobDetailVO job = jobs.get(a.getJobId());
            if (job != null) {
                merge(byCity, job.getCity());
            }
        }
        return toVOs(byCity);
    }

    /** null=不受限（ADMIN，全租户）；否则为可见学生 userId 集合 */
    private Set<Long> visibleIds() {
        return scopeService.visibleStudentIds(UserContextHolder.getUserId(), UserContextHolder.getRole());
    }

    private void merge(Map<String, Integer> map, String city) {
        if (city != null && !city.trim().isEmpty()) {
            map.merge(city.trim(), 1, Integer::sum);
        }
    }

    /** city→count 转成带坐标的 VO，跳过无坐标城市，按 count 降序 */
    private List<MapCityCountVO> toVOs(Map<String, Integer> byCity) {
        return byCity.entrySet().stream()
                .map(e -> {
                    Optional<double[]> coord = CityGeoUtil.resolve(e.getKey());
                    if (!coord.isPresent()) {
                        return null;
                    }
                    MapCityCountVO vo = new MapCityCountVO();
                    vo.setCityName(e.getKey());
                    vo.setLongitude(coord.get()[0]);
                    vo.setLatitude(coord.get()[1]);
                    vo.setCount(e.getValue());
                    return vo;
                })
                .filter(java.util.Objects::nonNull)
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }
}
