package com.occupation.common.config;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 全局 JSON 序列化配置：把 <b>id 类的 Long 字段</b>序列化成字符串。
 * <p>
 * <b>为什么必须转：</b>主键用雪花算法（{@code IdType.ASSIGN_ID}），运行时生成的 id 是 19 位
 * 大整数（约 2e18）。JavaScript 的 Number 只能精确表示到 2^53（约 9e15），超过就<b>静默丢失
 * 尾数</b>——后端返回 {@code 2076547053897310210}，浏览器 {@code JSON.parse} 后变成
 * {@code 2076547053897310200}。前端拿这个被改过的 id 去请求（下载 / 删除 / 详情），后端按 id
 * 查不到 → 表现为「报告不存在」「点了没反应」。种子数据 id 小（1、2、5…）不触发，所以这个坑
 * 潜伏到有人下载运行时生成的报告才暴露。
 * <p>
 * <b>为什么只转 id 而不是所有 Long：</b>把<i>全部</i> Long 转字符串会误伤计数字段——
 * 例如 {@code DashboardVO.DimensionItem.count}（Long）喂给图表，而 {@code Dashboard.vue} 里有
 * {@code sum + (i.count || 0)}，字符串会变成拼接、把饼图聚合弄坏。因此这里只对<b>名为 {@code id}
 * 或以 {@code Id} 结尾</b>的 {@link Long} 字段转字符串（id、userId、jobId、classId、reportId、
 * tenantId…），计数/数值（count、jobCount、value）保持数字。{@code PageResult} 的
 * {@code total/pageNum/pageSize} 是基本类型 {@code long}、不匹配包装类，天然不受影响。
 *
 * @author occupation-team
 */
@Configuration
public class JacksonConfig {

    /** Spring Boot 会自动把所有 {@link Module} 类型的 Bean 注册进 Jackson 的 ObjectMapper */
    @Bean
    public Module idLongToStringModule() {
        SimpleModule module = new SimpleModule("idLongToString");
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                             BeanDescription beanDesc,
                                                             List<BeanPropertyWriter> beanProperties) {
                for (BeanPropertyWriter writer : beanProperties) {
                    if (isIdLong(writer)) {
                        writer.assignSerializer(ToStringSerializer.instance);
                    }
                }
                return beanProperties;
            }
        });
        return module;
    }

    /** 判断是否为「id 类的 Long 包装字段」：类型是 java.lang.Long 且字段名为 id 或以 Id 结尾 */
    private static boolean isIdLong(BeanPropertyWriter writer) {
        if (!Long.class.equals(writer.getType().getRawClass())) {
            return false;
        }
        String name = writer.getName();
        return "id".equals(name) || name.endsWith("Id") || name.endsWith("id");
    }
}
