package com.occupation.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.report.entity.ReportDelivery;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 报告下发记录 Mapper
 *
 * @author occupation-team
 */
@Mapper
public interface ReportDeliveryMapper extends BaseMapper<ReportDelivery> {

    /**
     * 物理删除某报告的全部下发行（含逻辑删除的残留）。
     * <p>下发采用「覆盖重建」，而本表有唯一键 {@code uk_report_user}；
     * 若用 MyBatis-Plus 的逻辑删除，残留的 {@code deleted=1} 行会卡住重新下发（唯一键冲突）。
     * 因此重建前必须物理清空。
     */
    @Delete("DELETE FROM report_delivery WHERE report_id = #{reportId}")
    int hardDeleteByReport(@Param("reportId") Long reportId);
}
