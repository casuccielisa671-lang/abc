-- ============================================================
-- 增量升级脚本：新增 student_resume（学生简历表）
-- 生成日期: 2026-07-10
-- ============================================================
--
-- 用途：给「已经有数据、不想 docker-compose down -v 重建」的库补上这张表。
-- 全新的库不需要执行本脚本 —— init.sql 里已经包含同样的 DDL 与种子数据。
--
-- 本脚本刻意做到可重复执行且不破坏数据：
--   · CREATE TABLE IF NOT EXISTS —— 表已存在就跳过，绝不 DROP
--   · INSERT IGNORE             —— 种子行已存在就跳过（uk_user_id 兜底）
--
-- ⚠️ 本脚本刻意不写 `USE occupation;`：init.sql 里那句 USE 会覆盖命令行指定的库名，
--    是个已知的脚枪。请在命令行显式指定库名执行：
--
--   docker exec -i occupation-mysql mysql -uroot -proot occupation < upgrade-2026-07-10-student-resume.sql
--
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS student_resume (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '简历ID',
    tenant_id      BIGINT       NOT NULL COMMENT '所属租户ID',
    user_id        BIGINT       NOT NULL COMMENT '关联用户ID',
    contact_phone  VARCHAR(20)  DEFAULT NULL COMMENT '求职手机号（为空则用 sys_user.phone）',
    contact_email  VARCHAR(100) DEFAULT NULL COMMENT '求职邮箱（为空则用 sys_user.email）',
    job_intention  VARCHAR(100) DEFAULT NULL COMMENT '求职意向岗位，如「Java后端开发」',
    self_intro     TEXT         DEFAULT NULL COMMENT '自我评价',
    educations     TEXT         DEFAULT NULL COMMENT '教育经历 JSON 数组：[{school,major,degree,startDate,endDate,gpa}]',
    projects       TEXT         DEFAULT NULL COMMENT '项目经历 JSON 数组：[{name,role,startDate,endDate,description,skills}]',
    internships    TEXT         DEFAULT NULL COMMENT '实习经历 JSON 数组：[{company,position,startDate,endDate,description}]',
    honors         TEXT         DEFAULT NULL COMMENT '获奖与证书 JSON 数组：["ACM省赛二等奖","CET-6"]',
    ai_review      LONGTEXT     DEFAULT NULL COMMENT '最近一次 AI 诊断结果（JSON），避免重复调用大模型',
    ai_review_time DATETIME     DEFAULT NULL COMMENT 'AI 诊断时间',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生简历表';

-- ---------- 种子简历（与 init.sql 同源）----------
INSERT IGNORE INTO student_resume (id, tenant_id, user_id, contact_phone, contact_email, job_intention, self_intro, educations, projects, internships, honors) VALUES
(1, 1, 2, '13800000002', 'student@stu.test.edu.cn', 'Java后端开发工程师', '计算机科学与技术专业本科在读，熟悉 Java、Spring Boot、MySQL 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在杭州从事Java后端开发工程师相关工作。', '[{"school":"测试学院","major":"计算机科学与技术","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.08/4.0"}]', '[{"name":"计算机科学与技术综合实训 · Linux实践项目","role":"数据处理","startDate":"2025-03","endDate":"2025-07","description":"使用 Linux、Git、Java 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["Linux","Git","Java"]}]', '[]', '["全国大学生数学建模竞赛省二等奖","蓝桥杯省赛一等奖"]'),
(2, 1, 5, '13811000001', 'chenjiayi@stu.test.edu.cn', '后端开发工程师', '软件工程专业本科在读，熟悉 Java、MySQL、Vue 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在上海从事后端开发工程师相关工作。', '[{"school":"测试学院","major":"软件工程","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.22/4.0"}]', '[{"name":"软件工程综合实训 · Vue实践项目","role":"负责人","startDate":"2025-03","endDate":"2025-07","description":"使用 Vue、Java、JavaScript 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["Vue","Java","JavaScript"]},{"name":"校园信息服务平台","role":"组员","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 Git、JavaScript 相关模块开发，负责其中约 40% 的编码量。","skills":["Git","JavaScript"]}]', '[]', '["ACM-ICPC 省赛二等奖","校级一等奖学金","优秀学生干部"]'),
(3, 1, 6, '13811000002', 'lihaoran@stu.test.edu.cn', '大数据开发工程师', '数据科学与大数据技术专业本科在读，熟悉 Python、SQL、Spark 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在北京从事大数据开发工程师相关工作。', '[{"school":"测试学院","major":"数据科学与大数据技术","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.06/4.0"}]', '[{"name":"数据科学与大数据技术综合实训 · Spark实践项目","role":"数据处理","startDate":"2025-03","endDate":"2025-07","description":"使用 Spark、SQL、Hadoop 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["Spark","SQL","Hadoop"]},{"name":"校园实验管理平台","role":"核心开发","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 数据分析、Spark 相关模块开发，负责其中约 40% 的编码量。","skills":["数据分析","Spark"]}]', '[{"company":"云栖数智信息技术有限公司","position":"大数据开发工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["软件设计师（中级）","全国大学生数学建模竞赛省二等奖","蓝桥杯省赛一等奖"]'),
(4, 1, 7, '13811000003', 'wangyutong@stu.test.edu.cn', '算法工程师', '人工智能专业硕士在读，熟悉 Python、机器学习、深度学习 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在北京从事算法工程师相关工作。', '[{"school":"测试学院","major":"人工智能","degree":"硕士","startDate":"2023-09","endDate":"2026-06","gpa":"3.03/4.0"}]', '[{"name":"人工智能综合实训 · 深度学习实践项目","role":"后端开发","startDate":"2025-03","endDate":"2025-07","description":"使用 深度学习、SQL、PyTorch 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["深度学习","SQL","PyTorch"]},{"name":"校园信息服务平台","role":"核心开发","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 机器学习、SQL 相关模块开发，负责其中约 40% 的编码量。","skills":["机器学习","SQL"]}]', '[{"company":"乐游互动娱乐有限公司","position":"算法工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["“互联网+”大学生创新创业大赛校赛金奖","校级一等奖学金"]'),
(5, 1, 8, '13811000004', 'zhangzimo@stu.test.edu.cn', 'Java后端开发工程师', '计算机科学与技术专业本科在读，熟悉 C++、数据结构、Linux 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在深圳从事Java后端开发工程师相关工作。', '[{"school":"测试学院","major":"计算机科学与技术","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.78/4.0"}]', '[{"name":"计算机科学与技术综合实训 · 数据结构实践项目","role":"数据处理","startDate":"2025-03","endDate":"2025-07","description":"使用 数据结构、C++、Python 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["数据结构","C++","Python"]},{"name":"校园数据分析平台","role":"组员","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 Linux、Python 相关模块开发，负责其中约 40% 的编码量。","skills":["Linux","Python"]}]', '[{"company":"闪购网络科技有限公司","position":"Java后端开发工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["国家励志奖学金"]'),
(6, 1, 9, '13811000005', 'liusiyuan@stu.test.edu.cn', '安全工程师', '信息安全专业本科在读，熟悉 Linux、Python、网络安全 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在深圳从事安全工程师相关工作。', '[{"school":"测试学院","major":"信息安全","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.20/4.0"}]', '[{"name":"信息安全综合实训 · 网络安全实践项目","role":"后端开发","startDate":"2025-03","endDate":"2025-07","description":"使用 网络安全、Docker、Linux 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["网络安全","Docker","Linux"]}]', '[{"company":"云栖数智信息技术有限公司","position":"安全工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["国家励志奖学金","优秀学生干部","全国大学生数学建模竞赛省二等奖"]'),
(7, 1, 11, '13811000007', 'zhaoyiming@stu.test.edu.cn', '后端开发工程师', '软件工程专业本科在读，熟悉 Java、Spring Boot、微服务 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在南京从事后端开发工程师相关工作。', '[{"school":"测试学院","major":"软件工程","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.16/4.0"}]', '[{"name":"软件工程综合实训 · Spring Boot实践项目","role":"后端开发","startDate":"2025-03","endDate":"2025-07","description":"使用 Spring Boot、微服务、Java 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["Spring Boot","微服务","Java"]}]', '[{"company":"云栖数智信息技术有限公司","position":"后端开发工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["“互联网+”大学生创新创业大赛校赛金奖","校级一等奖学金"]'),
(8, 1, 13, '13811000009', 'zhoujunjie@stu.test.edu.cn', '前端开发工程师', '计算机应用技术专业专科在读，熟悉 JavaScript、Vue、CSS 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在成都从事前端开发工程师相关工作。', '[{"school":"测试学院","major":"计算机应用技术","degree":"专科","startDate":"2022-09","endDate":"2026-06","gpa":"3.12/4.0"}]', '[{"name":"计算机应用技术综合实训 · JavaScript实践项目","role":"负责人","startDate":"2025-03","endDate":"2025-07","description":"使用 JavaScript、Git、CSS 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["JavaScript","Git","CSS"]},{"name":"校园竞赛训练平台","role":"组员","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 JavaScript、Vue 相关模块开发，负责其中约 40% 的编码量。","skills":["JavaScript","Vue"]}]', '[{"company":"华信云科技有限公司","position":"前端开发工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["优秀学生干部","“互联网+”大学生创新创业大赛校赛金奖"]');
