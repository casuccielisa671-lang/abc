package com.occupation.recommend.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.service.UserService;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教师端接口 — 学生就业动态与教学建议
 * <p>
 * 数据范围：当前租户（本校）内的学生，多租户插件自动隔离。
 *
 * @author occupation-team
 */
@Slf4j
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
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

    /** 本校学生列表（分页，含真实姓名/学号与行为计数） */
    @GetMapping("/students")
    public Result<PageResult<StudentVO>> listStudents(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String education,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        Page<SysStudentProfile> profilePage = profileService.pageProfiles(keyword, education, page, size);
        return Result.ok(PageResult.of(profilePage.getTotal(), profilePage.getCurrent(),
                profilePage.getSize(), toStudentVOs(profilePage.getRecords())));
    }

    /** 班级概览统计（统计卡片） */
    @GetMapping("/overview")
    public Result<TeacherOverviewVO> overview() {
        Map<String, Long> counts = behaviorService.countByActionForTenant();
        TeacherOverviewVO vo = new TeacherOverviewVO();
        vo.setTotalStudents(userService.countByRole("STUDENT"));
        vo.setWithProfile(profileService.listAll().size());
        vo.setTotalViews(counts.getOrDefault(BehaviorAction.VIEW, 0L));
        vo.setTotalApplies(counts.getOrDefault(BehaviorAction.APPLY, 0L));
        vo.setTotalContacts(counts.getOrDefault(BehaviorAction.CONTACT, 0L));
        return Result.ok(vo);
    }

    /** 指定学生的求职行为统计 */
    @GetMapping("/students/{userId}/stats")
    public Result<BehaviorStatsVO> studentStats(@PathVariable Long userId) {
        return Result.ok(BehaviorStatsVO.of(
                behaviorService.countByAction(userId),
                behaviorService.listByUser(userId, 1)));
    }

    /** 指定学生的行为明细（最近 50 条，附关联职位的标题与公司） */
    @GetMapping("/students/{userId}/behaviors")
    public Result<List<BehaviorVO>> studentBehaviors(@PathVariable Long userId) {
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

    /** 教学建议 — 技能缺口诊断（市场热度 vs 学生掌握率） */
    @GetMapping("/suggestions")
    public Result<TeachingSuggestionVO> suggestions() {
        return Result.ok(suggestionService.diagnose(DIAGNOSE_TOP_SKILLS, DIAGNOSE_MAX_ITEMS));
    }

    /**
     * 教学建议的 AI 解读 —— 把上面那张技能缺口表翻译成一段可执行的教学调整建议。
     * <p>
     * 单独一个接口而不是塞进 {@link #suggestions()}：调大模型要几秒，
     * 不能让教师每次打开页面都等；表格先出来，解读按需再拉。
     */
    @GetMapping("/suggestions/ai")
    public Result<AdvisorReplyVO> suggestionsAi() {
        return Result.ok(teachingAiService.analyze(
                suggestionService.diagnose(DIAGNOSE_TOP_SKILLS, DIAGNOSE_MAX_ITEMS)));
    }

    /** 导出本校学生就业数据 Excel（.xlsx） */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        // 导出全量，不分页
        List<SysStudentProfile> profiles = profileService.listAll();
        List<StudentVO> students = toStudentVOs(profiles);

        List<Map<String, Object>> rows = new ArrayList<>(students.size());
        for (StudentVO s : students) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("学号", s.getUsername());
            row.put("姓名", s.getRealName());
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
                        "学号", "姓名", "专业", "学历", "技能", "意向城市", "意向行业",
                        "期望薪资", "浏览次数", "收藏次数", "投递次数"));
            } else {
                writer.write(rows, true);
            }
            writer.flush(out, true);
        }
        log.info("导出学生就业数据: {} 行", rows.size());
    }

    /**
     * 画像 → StudentVO：批量补齐 sys_user 的姓名/学号与行为计数，避免逐条查库。
     */
    private List<StudentVO> toStudentVOs(List<SysStudentProfile> profiles) {
        if (profiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = profiles.stream()
                .map(SysStudentProfile::getUserId).collect(Collectors.toList());
        Map<Long, SysUser> users = userService.mapByIds(userIds);
        Map<Long, Map<String, Long>> behaviors = behaviorService.countByActionGroupedByUser(userIds);

        return profiles.stream().map(p -> {
            SysUser u = users.get(p.getUserId());
            return StudentVO.of(p,
                    u == null ? null : u.getUsername(),
                    u == null ? null : u.getRealName(),
                    behaviors.getOrDefault(p.getUserId(), Collections.emptyMap()));
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
