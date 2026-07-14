package com.occupation.recommend.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.auth.entity.SysClass;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.service.ClassService;
import com.occupation.auth.service.TeacherScopeService;
import com.occupation.auth.service.UserService;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.entity.BehaviorAction;
import com.occupation.recommend.entity.StudentBehavior;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.service.TeachingAiService;
import com.occupation.recommend.service.TeachingSuggestionService;
import com.occupation.recommend.vo.AdvisorReplyVO;
import com.occupation.recommend.vo.BehaviorStatsVO;
import com.occupation.recommend.vo.BehaviorVO;
import com.occupation.recommend.vo.StudentVO;
import com.occupation.recommend.vo.TeacherOverviewVO;
import com.occupation.recommend.vo.TeachingSuggestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师端接口 — 学生就业动态与教学建议
 * <p>
 * 数据范围：先经多租户插件隔离到本校，再按<b>教师可见范围</b>（班主任/专业老师/届老师）
 * 二次收窄。所有学生数据接口都必须走 {@link #visibleScope(String, Integer)} 解析出的
 * 可见学生集合，否则任何教师都能看全校学生。管理员（ADMIN）不受范围限制。
 *
 * @author occupation-team
 */
@Slf4j
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    /** 技能缺口诊断参与对比的市场热门技能数 */
    private static final int DIAGNOSE_TOP_SKILLS = 50;
    /** 技能缺口诊断返回的条目上限 */
    private static final int DIAGNOSE_MAX_ITEMS = 10;
    /** 学生行为明细返回条数 */
    private static final int BEHAVIOR_DETAIL_LIMIT = 50;

    private final StudentProfileService profileService;
    private final BehaviorService behaviorService;
    private final TeachingSuggestionService suggestionService;
    private final TeachingAiService teachingAiService;
    private final UserService userService;
    private final JobDetailService jobDetailService;
    private final TeacherScopeService scopeService;
    private final ClassService classService;
    private final com.occupation.recommend.service.TeacherMapService teacherMapService;

    /** 本校学生列表（分页，含真实姓名/学号、班级与行为计数；按教师范围过滤，支持专业/年级二次筛选） */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/students")
    public Result<PageResult<StudentVO>> listStudents(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String education,
                                                      @RequestParam(required = false) String major,
                                                      @RequestParam(required = false) Integer enrollYear,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        Collection<Long> restrict = visibleScope(major, enrollYear);
        Page<SysStudentProfile> profilePage =
                profileService.pageProfiles(keyword, education, restrict, page, size);
        return Result.ok(PageResult.of(profilePage.getTotal(), profilePage.getCurrent(),
                profilePage.getSize(), toStudentVOs(profilePage.getRecords())));
    }

    /** 班级概览统计（统计卡片；数字按教师可见范围计算） */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/overview")
    public Result<TeacherOverviewVO> overview() {
        Set<Long> visible = visibleStudents();
        TeacherOverviewVO vo = new TeacherOverviewVO();

        vo.setTotalStudents(visible == null ? userService.countByRole("STUDENT") : visible.size());
        vo.setWithProfile(profileService.listByUserIds(visible).size());

        Map<String, Long> counts = (visible == null)
                ? behaviorService.countByActionForTenant()
                : sumByAction(behaviorService.countByActionGroupedByUser(visible));
        vo.setTotalViews(counts.getOrDefault(BehaviorAction.VIEW, 0L));
        vo.setTotalApplies(counts.getOrDefault(BehaviorAction.APPLY, 0L));
        vo.setTotalContacts(counts.getOrDefault(BehaviorAction.CONTACT, 0L));
        return Result.ok(vo);
    }

    /** 指定学生的求职行为统计（带范围归属校验） */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/students/{userId}/stats")
    public Result<BehaviorStatsVO> studentStats(@PathVariable Long userId) {
        assertVisible(userId);
        return Result.ok(BehaviorStatsVO.of(
                behaviorService.countByAction(userId),
                behaviorService.listByUser(userId, 1)));
    }

    /** 指定学生的行为明细（最近 50 条，附关联职位的标题与公司；带范围归属校验） */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/students/{userId}/behaviors")
    public Result<List<BehaviorVO>> studentBehaviors(@PathVariable Long userId) {
        assertVisible(userId);
        List<StudentBehavior> behaviors = behaviorService.listByUser(userId, BEHAVIOR_DETAIL_LIMIT);
        if (behaviors.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 一次批量取回涉及的职位，避免逐条查库
        List<Long> jobIds = behaviors.stream()
                .map(StudentBehavior::getJobId).distinct().collect(Collectors.toList());
        Map<Long, JobDetailVO> jobs = jobDetailService.listByIds(jobIds).stream()
                .collect(Collectors.toMap(JobDetailVO::getId, j -> j));

        return Result.ok(behaviors.stream()
                .map(b -> BehaviorVO.of(b, jobs.get(b.getJobId())))
                .collect(Collectors.toList()));
    }

    /** 教学建议 — 技能缺口诊断（市场热度 vs 学生掌握率；学生样本按教师范围限定） */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/suggestions")
    public Result<TeachingSuggestionVO> suggestions() {
        return Result.ok(suggestionService.diagnose(DIAGNOSE_TOP_SKILLS, DIAGNOSE_MAX_ITEMS, visibleStudents()));
    }

    /**
     * 教学建议的 AI 解读 —— 把上面那张技能缺口表翻译成一段可执行的教学调整建议。
     * <p>
     * 单独一个接口而不是塞进 {@link #suggestions()}：调大模型要几秒，
     * 不能让教师每次打开页面都等；表格先出来，解读按需再拉。
     */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/suggestions/ai")
    public Result<AdvisorReplyVO> suggestionsAi() {
        return Result.ok(teachingAiService.analyze(
                suggestionService.diagnose(DIAGNOSE_TOP_SKILLS, DIAGNOSE_MAX_ITEMS, visibleStudents())));
    }

    /** 地图图层：学生求职意向城市分布（按可见范围） */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/map/intent-cities")
    public Result<java.util.List<com.occupation.recommend.vo.MapCityCountVO>> intentCities() {
        return Result.ok(teacherMapService.studentIntentCities());
    }

    /** 地图图层：投递去向城市分布（按可见范围） */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/map/application-cities")
    public Result<java.util.List<com.occupation.recommend.vo.MapCityCountVO>> applicationCities() {
        return Result.ok(teacherMapService.applicationCities());
    }

    /** 可选筛选项：教师可见范围内涉及的专业 / 入学年级（前端筛选下拉用） */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/filters")
    public Result<Map<String, Object>> filters() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("majors", classService.listMajors());
        data.put("years", classService.listYears());
        return Result.ok(data);
    }

    /** 教师可见范围内的班级列表（工具箱班级对比下拉用） */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/classes")
    public Result<List<Map<String, Object>>> classes() {
        Set<Long> visible = visibleStudents();
        List<SysClass> all = classService.listAll();
        if (visible == null) {
            // ADMIN：返回全部班级
            return Result.ok(all.stream().map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.getId());
                m.put("name", c.getCode());
                m.put("major", c.getMajor());
                m.put("enrollYear", c.getEnrollYear());
                m.put("className", c.getClassName());
                return m;
            }).collect(Collectors.toList()));
        }
        // 教师：只返回可见范围内有学生的班级
        Set<Long> classIds = userService.mapByIds(visible).values().stream()
                .map(SysUser::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        return Result.ok(all.stream()
                .filter(c -> classIds.contains(c.getId()))
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getCode());
                    m.put("major", c.getMajor());
                    m.put("enrollYear", c.getEnrollYear());
                    m.put("className", c.getClassName());
                    return m;
                }).collect(Collectors.toList()));
    }

    /** 导出学生就业数据 Excel（.xlsx；按教师可见范围导出） */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        // 导出范围内全量，不分页
        List<SysStudentProfile> profiles = profileService.listByUserIds(visibleStudents());
        List<StudentVO> students = toStudentVOs(profiles);

        List<Map<String, Object>> rows = new ArrayList<>(students.size());
        for (StudentVO s : students) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("学号", s.getUsername());
            row.put("姓名", s.getRealName());
            row.put("班级", s.getClassCode());
            row.put("专业", s.getMajor());
            row.put("学历", s.getEducationLevel());
            row.put("技能", String.join("、", SkillUtils.parse(s.getSkills())));
            row.put("意向城市", s.getExpectedCity());
            row.put("意向行业", s.getExpectedIndustry());
            row.put("期望薪资", formatSalary(s.getExpectedSalaryMin(), s.getExpectedSalaryMax()));
            row.put("浏览次数", s.getViewCount());
            row.put("收藏次数", s.getFavoriteCount());
            row.put("投递次数", s.getApplyCount());
            rows.add(row);
        }

        String filename = "学生就业数据_" + LocalDate.now() + ".xlsx";
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // RFC 5987：中文文件名在 Chrome/Edge/Firefox 下均需 URL 编码，否则下载后变乱码
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));

        // isXlsx=true：POI 的 XSSF 格式，与 .xlsx 扩展名一致
        try (ExcelWriter writer = ExcelUtil.getWriter(true);
             OutputStream out = response.getOutputStream()) {
            if (rows.isEmpty()) {
                writer.writeHeadRow(java.util.Arrays.asList(
                        "学号", "姓名", "班级", "专业", "学历", "技能", "意向城市", "意向行业",
                        "期望薪资", "浏览次数", "收藏次数", "投递次数"));
            } else {
                writer.write(rows, true);
            }
            writer.flush(out, true);
        }
        log.info("导出学生就业数据: {} 行", rows.size());
    }

    // ==================== 教师可见范围 ====================

    /** 当前登录教师的可见学生集合：null=不受限（ADMIN）；空集=看不到；否则为可见 userId 集合 */
    private Set<Long> visibleStudents() {
        return scopeService.visibleStudentIds(UserContextHolder.getUserId(), UserContextHolder.getRole());
    }

    /**
     * 在教师可见范围基础上，叠加专业/年级二次筛选，得到最终限定的学生集合。
     * @return null=不限制（ADMIN 且无筛选）；否则为交集（可能为空集）
     */
    private Collection<Long> visibleScope(String major, Integer enrollYear) {
        Set<Long> visible = visibleStudents();
        Set<Long> byFilter = classService.studentIdsByMajorYear(major, enrollYear);
        if (visible == null) {
            return byFilter;      // ADMIN：仅受筛选限制（byFilter 为 null 即不限制）
        }
        if (byFilter == null) {
            return visible;       // 无筛选：仅受范围限制
        }
        Set<Long> inter = new HashSet<>(visible);
        inter.retainAll(byFilter);
        return inter;
    }

    /** 范围归属校验：目标学生不在可见范围内则 403（防止枚举 userId 越权查看他班学生） */
    private void assertVisible(Long userId) {
        Set<Long> visible = visibleStudents();
        if (visible != null && !visible.contains(userId)) {
            log.warn("拒绝越权查看学生: teacherId={}, targetUserId={}", UserContextHolder.getUserId(), userId);
            throw new BizException(403, "无权查看该学生：不在你的可见范围内");
        }
    }

    /** 把「按用户分组的行为计数」汇总成「按行为类型的总计数」 */
    private Map<String, Long> sumByAction(Map<Long, Map<String, Long>> grouped) {
        Map<String, Long> total = new java.util.HashMap<>();
        for (Map<String, Long> perUser : grouped.values()) {
            perUser.forEach((action, cnt) -> total.merge(action, cnt, Long::sum));
        }
        return total;
    }

    /**
     * 画像 → StudentVO：批量补齐 sys_user 的姓名/学号、班级信息与行为计数，避免逐条查库。
     */
    private List<StudentVO> toStudentVOs(List<SysStudentProfile> profiles) {
        if (profiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = profiles.stream()
                .map(SysStudentProfile::getUserId).collect(Collectors.toList());
        Map<Long, SysUser> users = userService.mapByIds(userIds);
        Map<Long, Map<String, Long>> behaviors = behaviorService.countByActionGroupedByUser(userIds);
        // 批量取回班级信息
        Set<Long> classIds = users.values().stream()
                .map(SysUser::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysClass> classes = classService.mapByIds(classIds);

        return profiles.stream().map(p -> {
            SysUser u = users.get(p.getUserId());
            StudentVO vo = StudentVO.of(p,
                    u == null ? null : u.getUsername(),
                    u == null ? null : u.getRealName(),
                    behaviors.getOrDefault(p.getUserId(), Collections.emptyMap()));
            if (u != null && u.getClassId() != null) {
                vo.setClassId(u.getClassId());
                SysClass c = classes.get(u.getClassId());
                if (c != null) {
                    vo.setClassCode(c.getCode());
                    vo.setEnrollYear(c.getEnrollYear());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String formatSalary(Integer min, Integer max) {
        if (min == null && max == null) {
            return "";
        }
        if (max == null) {
            return min + " 起";
        }
        if (min == null) {
            return "至 " + max;
        }
        return min + " - " + max;
    }
}
