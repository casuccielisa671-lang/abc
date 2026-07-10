package com.occupation.recommend.entity;

/**
 * 学生行为类型 — {@code student_behavior.action} 的取值
 * <p>
 * 这是<b>行为埋点</b>的分类，不是业务状态。真正的投递流程见 {@link ApplicationStatus}。
 * <p>
 * <b>VIEW 允许重复记录</b>（反映活跃度），其余四种在 Service 层做幂等。
 *
 * @author occupation-team
 */
public final class BehaviorAction {

    private BehaviorAction() {
    }

    /** 浏览职位详情。唯一允许重复记录的行为 */
    public static final String VIEW = "VIEW";

    /** 收藏 */
    public static final String FAVORITE = "FAVORITE";

    /** 站内投递。仅 HR 发布的职位可投，同时会写一条 job_application */
    public static final String APPLY = "APPLY";

    /** 不感兴趣。负信号，会拉低相似职位的推荐分 */
    public static final String IGNORE = "IGNORE";

    /**
     * 自主联系 —— 学生对采集来的「市场参考」职位表达求职意向，跳出平台自行联系。
     * <p>
     * 与 {@link #APPLY} 的区别：投递的另一端有 HR 在平台上处理，有状态机；
     * 自主联系之后发生了什么平台无从得知，只能记录意向本身。
     * <p>
     * 权重与 APPLY 同级：学生愿意跳出平台去联系，意愿强度不弱于站内投递。
     */
    public static final String CONTACT = "CONTACT";
}
