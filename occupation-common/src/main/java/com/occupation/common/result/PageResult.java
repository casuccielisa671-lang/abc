package com.occupation.common.result;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应体
 * <p>
 * 所有分页查询接口统一返回此结构，配合 {@link Result} 使用：
 * {@code Result.ok(PageResult.of(page))}
 *
 * @param <T> 列表元素类型
 * @author occupation-team
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long pageNum;

    /** 每页条数 */
    private long pageSize;

    /** 当前页数据 */
    private List<T> list;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> r = new PageResult<>();
        r.total = page.getTotal();
        r.pageNum = page.getCurrent();
        r.pageSize = page.getSize();
        r.list = page.getRecords();
        return r;
    }

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.total = total;
        r.pageNum = pageNum;
        r.pageSize = pageSize;
        r.list = list;
        return r;
    }
}
