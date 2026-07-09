package com.occupation.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 批量导入用户的结果报告
 * <p>
 * 采用「先全量校验，再整体入库」的策略：只要有一行不合法就不写库，
 * 由管理员改好 Excel 再传一次。这样避免导入一半、名单对不上的尴尬状态。
 *
 * @author occupation-team
 */
@Data
public class BatchImportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 解析到的数据行数（不含表头） */
    private int total;

    /** 成功导入数（校验未通过时为 0） */
    private int imported;

    /** 校验失败的行 */
    private List<RowError> errors = new ArrayList<>();

    /** 未在 Excel 中填写密码的账号所使用的初始密码；全部自带密码时为 null */
    private String defaultPassword;

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void addError(int rowNum, String username, String reason) {
        errors.add(new RowError(rowNum, username, reason));
    }

    /** 一行的校验错误 */
    @Data
    public static class RowError implements Serializable {
        private static final long serialVersionUID = 1L;

        /** Excel 中的行号（含表头，从 1 开始，与用户在 Excel 里看到的一致） */
        private final int rowNum;
        private final String username;
        private final String reason;
    }
}
