/**
 * 就业资讯 mock 数据
 *
 * 图片目录：occupation-web-ui/public/newsImages/
 * 引用路径：/newsImages/文件名
 */

export const NEWS_CATEGORIES = [
  { id: 'all', label: '全部' },
  { id: 'backend', label: '后端开发' },
  { id: 'frontend', label: '前端开发' },
  { id: 'test', label: '测试开发' },
  { id: 'devops', label: '运维开发' },
  { id: 'bigdata', label: '大数据开发' }
]

const PLACEHOLDER = 'https://images.unsplash.com/photo-1521737710461-ddcc3bb805bb?w=800&q=80'

export const jobNewsList = [
  {
    id: 1,
    category: 'backend',
    tag: '岗位介绍',
    title: '2026 春招后端开发工程师能力模型与成长路径',
    summary: 'Java / Go 双栈需求并存，微服务、分布式与高并发成为核心考察点。',
    content: '随着企业数字化深入，后端开发岗位持续细分。Spring Cloud、Kubernetes 与消息队列是主流技术栈；初级工程师需扎实掌握 RESTful API 与数据库设计，中级需理解缓存、限流与可观测性，高级则承担架构演进与稳定性治理。',
    image: '/newsImages/banner-backend.jpg',
    placeholder: PLACEHOLDER,
    time: '10:32',
    hot: '89.2万',
    featured: true
  },
  {
    id: 2,
    category: 'frontend',
    tag: '企业招聘',
    title: '头部互联网公司开放前端专项实习：Vue3 + 可视化方向',
    summary: '多地同步招聘，重视 Three.js / ECharts 数据可视化经验。',
    content: '招聘面向 2026 届及优秀 2027 届学生，要求熟悉 Vue3 组合式 API、TypeScript 与工程化工具链。可视化大屏、地图热力、性能优化为加分项。',
    image: '/newsImages/card-frontend.jpg',
    placeholder: 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=600&q=80',
    time: '09:15',
    hot: '56.8万'
  },
  {
    id: 3,
    category: 'bigdata',
    tag: '行业前景',
    title: '大数据开发岗位薪资报告：实时计算工程师涨幅领先',
    summary: 'Flink / Spark 技能溢价明显，金融与电商领域需求旺盛。',
    content: '季度薪酬调研显示，具备实时数仓与湖仓一体经验的工程师薪资中位数同比上涨 12%。建议掌握 Kafka、Flink SQL 及数据质量治理框架。',
    image: '/newsImages/card-bigdata.jpg',
    placeholder: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=600&q=80',
    time: '08:40',
    hot: '42.1万'
  },
  {
    id: 4,
    category: 'test',
    tag: '就业政策',
    title: '多地推出 ICT 人才补贴：测试开发纳入紧缺工种目录',
    summary: '自动化测试、性能测试与安全测试人才可申请专项补贴。',
    content: '若干省市更新紧缺职业目录，测试开发工程师纳入其中。自动化框架（Pytest、Playwright）与 CI/CD 集成能力成为企业筛选重点。',
    image: '/newsImages/card-test.jpg',
    placeholder: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600&q=80',
    time: '昨天',
    hot: '31.5万'
  },
  {
    id: 5,
    category: 'devops',
    tag: '技术发展',
    title: '云原生运维岗位激增：DevOps 工程师如何构建竞争力',
    summary: 'IaC、GitOps 与 AIOps 成为运维开发新标配。',
    content: '传统运维向 DevOps/SRE 转型加速。Terraform、Ansible、Prometheus + Grafana 为常见技术组合；平台工程岗位兴起。',
    image: '/newsImages/card-devops.jpg',
    placeholder: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80',
    time: '昨天',
    hot: '28.3万'
  },
  {
    id: 6,
    category: 'backend',
    tag: '企业招聘',
    title: '金融科技企业批量开放 Java 后端社招与校招',
    summary: '分布式事务、支付清算与风控系统为核心业务线。',
    content: '多家金融科技公司发布联合招聘计划，熟悉 MySQL 分库分表、Redis 集群及消息可靠投递者优先。',
    image: '/newsImages/card-backend2.jpg',
    placeholder: PLACEHOLDER,
    time: '2天前',
    hot: '22.7万'
  },
  {
    id: 7,
    category: 'frontend',
    tag: '行业前景',
    title: '低代码与 AI 辅助编程重塑前端岗位技能图谱',
    summary: '工程师需从「写页面」转向「设计体验与架构」。',
    content: 'AI 代码助手普及后，前端岗位更强调交互设计、无障碍、国际化与跨端一致性。',
    image: '/newsImages/card-frontend2.jpg',
    placeholder: 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=600&q=80',
    time: '2天前',
    hot: '19.4万'
  },
  {
    id: 8,
    category: 'bigdata',
    tag: '岗位介绍',
    title: '数据工程师 vs 算法工程师：职责边界与选型建议',
    summary: '数据平台建设与模型训练分工日益清晰。',
    content: '数据工程师负责管道、存储与指标；算法工程师聚焦特征与模型。交叉技能包括 SQL 优化、特征工程与 MLOps。',
    image: '/newsImages/card-bigdata2.jpg',
    placeholder: 'https://images.unsplash.com/photo-1504868584819-f8e8b4b6d7e3?w=600&q=80',
    time: '3天前',
    hot: '15.2万'
  }
]

export const hotRankList = jobNewsList
  .slice()
  .sort((a, b) => parseFloat(b.hot) - parseFloat(a.hot))
  .slice(0, 5)

export const flashNewsList = [
  { time: '10:15', text: '教育部：2026 届 ICT 类专场招聘会将于下月启动' },
  { time: '09:48', text: '华为、阿里等多家名企发布春季实习生招聘计划' },
  { time: '09:20', text: '人工智能训练师纳入国家新职业目录' },
  { time: '08:55', text: '多地放宽落户：紧缺工程技术人才可直接落户' },
  { time: '08:30', text: 'Spring Boot 3.3 LTS 发布，企业升级窗口开启' }
]

export const hotTopics = [
  { tag: '#春招后端', count: '12.4万讨论' },
  { tag: '#Vue3就业', count: '8.7万讨论' },
  { tag: '#大数据实习', count: '6.2万讨论' },
  { tag: '#测试开发转码', count: '5.1万讨论' },
  { tag: '#DevOps认证', count: '4.3万讨论' },
  { tag: '#就业补贴', count: '3.8万讨论' }
]
