package com.occupation.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.report.vo.ReceivedReportVO;

/**
 * 报告下发服务 —— 管理员把就业报告「发送」给某范围学生；学生查看「收到的报告」。
 * <p>
 * 两条可见性规则（与产品决策一致）：
 * <ul>
 *   <li>市场行业报告（{@code category=MARKET}）发布即全体可见，走广播口径，不落 delivery 行；</li>
 *   <li>学生就业报告（{@code category=EMPLOYMENT}）按范围定向下发，每个接收学生落一行 delivery。</li>
 * </ul>
 *
 * @author occupation-team
 */
public interface ReportDeliveryService {

    /**
     * 把报告发给某范围内的学生。
     *
     * @param reportId    报告 id（必须是租户级、已生成成功的就业报告）
     * @param targetType  范围类型：ALL=全体 / MAJOR=专业 / GRADE=入学年级 / CLASS=班级
     * @param targetValue 范围值：MAJOR=专业名、GRADE=年级数字、CLASS=班级 id；ALL 忽略
     * @return 本次新增下发的学生人数（已发过的不重复）
     */
    int deliver(Long reportId, String targetType, String targetValue);

    /** 该报告已下发的学生人数 */
    long deliveredCount(Long reportId);

    /** 某学生「收到的报告」分页：广播的市场报告 + 定向下发给他的报告，按时间倒序合并 */
    Page<ReceivedReportVO> receivedFor(Long userId, int pageNum, int pageSize);

    /** 学生是否有权访问某租户级报告（市场广播恒可 / 就业报告需已下发给他），供下载归属校验复用 */
    boolean canStudentAccess(Long reportId, Long userId);

    /** 标记该学生对某报告已读（仅对定向下发的报告有意义） */
    void markRead(Long reportId, Long userId);
}
