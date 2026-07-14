package com.occupation.recommend.controller;

import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.Result;
import com.occupation.recommend.dto.ResumePolishDTO;
import com.occupation.recommend.dto.ResumeSaveDTO;
import com.occupation.recommend.service.ResumeAiService;
import com.occupation.recommend.service.ResumeService;
import com.occupation.recommend.vo.ResumeReviewVO;
import com.occupation.recommend.vo.ResumeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 学生简历接口（学生端）
 * <p>
 * 简历只能读写自己的 —— userId 一律取自 Token，不接受前端传入。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/student/resume")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentResumeController {

    private final ResumeService resumeService;
    private final ResumeAiService resumeAiService;

    /** 我的简历（未填写返回 exists=false 的空壳，前端引导填写） */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping
    public Result<ResumeVO> getMyResume() {
        return Result.ok(resumeService.getByUserId(UserContextHolder.getUserId()));
    }

    /** 保存/更新简历 */
    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping
    public Result<Void> saveResume(@RequestBody @Validated ResumeSaveDTO dto) {
        resumeService.save(UserContextHolder.getUserId(), dto);
        return Result.ok();
    }

    /**
     * AI 简历诊断
     *
     * @param targetJobId 可选，对标某个职位诊断；不传则以市场热门技能为基准
     * @param refresh     true 强制重新调用大模型，false 命中缓存直接返回上次结果
     */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/ai-review")
    public Result<ResumeReviewVO> aiReview(@RequestParam(required = false) Long targetJobId,
                                           @RequestParam(defaultValue = "false") boolean refresh) {
        return Result.ok(resumeAiService.review(UserContextHolder.getUserId(), targetJobId, refresh));
    }

    /** AI 润色一段简历文字。返回 {polished: "..."}，前端决定是否采纳 */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/ai-polish")
    public Result<Map<String, String>> aiPolish(@RequestBody @Validated ResumePolishDTO dto) {
        String polished = resumeAiService.polish(dto.getSection(), dto.getText());
        return Result.ok(Collections.singletonMap("polished", polished));
    }
}
