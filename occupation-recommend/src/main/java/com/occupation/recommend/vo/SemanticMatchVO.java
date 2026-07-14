package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * JD-简历语义匹配出参。
 *
 * @author occupation-team
 */
@Data
public class SemanticMatchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 语义相似度 0~100 */
    private Integer similarity;

    /** 匹配点：JD 与简历中语义相近的技能/经验 */
    private List<String> matchedPoints;

    /** 差距点：JD 要求但简历中缺乏或语义不匹配的点 */
    private List<String> gapPoints;

    /** true=大模型生成，false=降级/缓存 */
    private boolean aiGenerated;
}
