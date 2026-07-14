/**
 * mock-jobs.json 生成器（确定性，可重复运行）
 *
 * 这个文件是「Mock 采集」的数据源：CrawlerServiceImpl 见到 source_type=MOCK 时，
 * 用 MockJobPageProcessor 读它，不发任何网络请求，但下游（Kafka → raw_job_data →
 * 清洗去重 → job_detail）与真实爬虫完全一致。
 *
 * 【定位（2026-07-14）】它产出的是「自主联系(采集)」市场参考数据（source=MOCK，
 * publisher_id=NULL）。开箱种子里已无采集职位，点采集任务才把这批数据引入平台。
 * 它是「市场标尺」，刻意比平台自己的 90 条可投递职位更宽：
 *   · 行业含平台没有的「医疗健康 / 新能源 / 企业服务」；
 *   · 公司是一批独立「市场公司」，与 gen-seed-data.js 的 12 家 HR 公司完全不重叠。
 *
 * ⚠️ sourceUrl 必须用 mock.local 域名。
 *    init.sql 里若还有种子职位用 mock.occupation.dev；清洗按 source_url 精确去重，
 *    两边撞了的话点「启动采集」一条职位都进不来。
 *
 * 用法：node scripts/gen-mock-jobs.js
 */
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'occupation-crawler', 'src', 'main', 'resources', 'mock', 'mock-jobs.json');
const COUNT = 100;

// ---------- 确定性随机（与 gen-seed-data.js 同一套，但独立种子）----------
function mulberry32(a) {
  return function () {
    a |= 0; a = (a + 0x6D2B79F5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const rnd = mulberry32(20260714);
const ri = (min, max) => min + Math.floor(rnd() * (max - min + 1));
const pick = (arr) => arr[Math.floor(rnd() * arr.length)];
const pickN = (arr, n) => {
  const copy = arr.slice(), out = [];
  for (let i = 0; i < n && copy.length; i++) out.push(copy.splice(Math.floor(rnd() * copy.length), 1)[0]);
  return out;
};
const pad2 = (n) => String(n).padStart(2, '0');
const pad3 = (n) => String(n).padStart(3, '0');

// ---------- 城市（加权，一线略高；权重保证 10 城都覆盖）----------
const CITIES = ['北京', '上海', '深圳', '杭州', '广州', '成都', '武汉', '南京', '西安', '苏州'];
const CITY_W = [16, 15, 13, 12, 9, 8, 7, 7, 6, 7];
const CITY_K = { 北京: 1.15, 上海: 1.15, 深圳: 1.1, 杭州: 1.05, 广州: 1.0, 成都: 0.9, 武汉: 0.88, 南京: 0.95, 西安: 0.85, 苏州: 0.95 };
const pickCity = () => {
  const total = CITY_W.reduce((a, b) => a + b, 0); let r = rnd() * total;
  for (let i = 0; i < CITIES.length; i++) { r -= CITY_W[i]; if (r <= 0) return CITIES[i]; }
  return CITIES[0];
};

// 18 家「市场公司」——与 gen-seed-data.js 的 12 家 HR 公司完全不重叠
const COMPANIES = [
  '千寻网络科技有限公司', '明道信息技术有限公司', '领航互联网络有限公司', '智橙科技有限公司',
  '卓越数据科技有限公司', '昇腾云计算有限公司', '星云智能科技有限公司', '微谷科技有限公司',
  '汇金融科技服务有限公司', '云链电商科技有限公司', '优学在线教育有限公司', '翼动游戏网络有限公司',
  '睿达工业智能有限公司', '恒峰医疗科技有限公司', '康泰健康信息有限公司', '绿能新能源科技有限公司',
  '鼎新软件技术有限公司', '慧通信息科技有限公司',
];

const EDUS = ['专科', '本科', '硕士'];
const EXPS = ['应届生', '1-3年', '3-5年', '经验不限'];
const EXP_K = { 应届生: 0.72, '1-3年': 1.0, '3-5年': 1.35, 经验不限: 0.9 };
const EDU_K = { 专科: 0.85, 本科: 1.0, 硕士: 1.2 };

// 岗位模板：[标题, 行业, 技能池, 薪资基准]
// 比平台宽：末尾三类「医疗健康 / 新能源 / 企业服务」是市场独有行业
const SPECS = [
  ['Java后端开发工程师', '互联网/IT', ['Java', 'Spring Boot', 'MySQL', 'Redis', '微服务', 'Git'], 12000],
  ['Python后端开发工程师', '互联网/IT', ['Python', 'Django', 'MySQL', 'Linux', 'Redis'], 12000],
  ['前端开发工程师', '互联网/IT', ['JavaScript', 'Vue', 'CSS', 'Git', 'TypeScript'], 11000],
  ['Go开发工程师', '互联网/IT', ['Go', 'MySQL', 'Redis', '微服务'], 13000],
  ['测试开发工程师', '互联网/IT', ['Python', 'Selenium', 'Linux', 'Git'], 10000],
  ['运维开发工程师', '互联网/IT', ['Linux', 'Docker', 'Kubernetes', 'Python'], 13000],
  ['信息安全工程师', '互联网/IT', ['Linux', '网络安全', 'Python', 'Docker'], 14000],
  ['大数据开发工程师', '大数据', ['Java', 'Spark', 'Hive', 'Kafka', 'Hadoop'], 15000],
  ['数据分析师', '大数据', ['SQL', 'Python', '数据分析', 'Excel', 'Tableau'], 10000],
  ['数据仓库工程师', '大数据', ['SQL', 'Hive', 'Spark', '数据建模'], 13000],
  ['算法工程师', '人工智能', ['Python', '机器学习', '深度学习', 'PyTorch'], 18000],
  ['自然语言处理工程师', '人工智能', ['Python', 'NLP', '深度学习', '机器学习'], 20000],
  ['计算机视觉工程师', '人工智能', ['Python', '计算机视觉', '深度学习', 'TensorFlow'], 18000],
  ['风控数据分析师', '金融', ['SQL', 'Python', '数理统计', '数据分析'], 14000],
  ['量化研究员', '金融', ['Python', '数理统计', '数据分析'], 22000],
  ['用户运营专员', '电子商务', ['用户运营', '数据分析', 'Excel', '文案策划'], 8000],
  ['电商产品经理', '电子商务', ['产品设计', '数据分析', '项目管理', 'Axure'], 12000],
  ['课程研发工程师', '教育', ['Java', 'Python', '数据分析'], 10000],
  ['游戏服务端开发', '游戏', ['C++', 'Linux', '分布式', 'Redis'], 16000],
  ['嵌入式软件工程师', '智能制造', ['C++', '单片机', 'Linux'], 11000],
  ['工业软件开发工程师', '智能制造', ['Java', 'MySQL', 'Linux'], 10000],
  // ---- 市场独有行业（平台 90 条可投递里没有）----
  ['医疗数据分析师', '医疗健康', ['Python', 'SQL', '数据分析'], 13000],
  ['医疗信息系统工程师', '医疗健康', ['Java', 'MySQL', 'Linux'], 11000],
  ['电池管理系统工程师', '新能源', ['C++', '单片机', '数据分析'], 13000],
  ['能源数据分析师', '新能源', ['SQL', 'Python', '数据分析'], 10000],
  ['SaaS后端开发工程师', '企业服务', ['Java', 'MySQL', '微服务'], 12000],
  ['ERP实施顾问', '企业服务', ['SQL', 'ERP', '项目管理'], 10000],
];

const jobs = [];
for (let i = 1; i <= COUNT; i++) {
  const [title, industry, skillPool, base] = SPECS[i % SPECS.length];
  const city = pickCity();
  const exp = pick(EXPS);
  const edu = pick(EDUS);
  const mid = base * CITY_K[city] * EXP_K[exp] * EDU_K[edu] * (0.92 + rnd() * 0.16);
  const salaryMin = Math.round((mid * 0.85) / 500) * 500;
  const salaryMax = Math.round((mid * 1.25) / 500) * 500;
  const skills = pickN(skillPool, ri(3, Math.min(5, skillPool.length)));
  const company = pick(COMPANIES);
  const month = ri(3, 7);
  const day = month === 7 ? ri(1, 9) : ri(1, 28);

  jobs.push({
    title: exp === '应届生' ? `${title}（校招）` : title,
    company,
    city,
    industry,
    salaryMin,
    salaryMax,
    education: edu,
    experience: exp,
    skills,
    description:
      `${company}诚聘${title}。工作地点：${city}。` +
      `岗位职责：参与${industry}方向核心业务系统的设计与开发，负责模块的编码、测试与线上维护。` +
      `任职要求：${edu}及以上学历，熟悉 ${skills.join('、')}；具备良好的编码规范与团队协作能力。` +
      `${exp === '应届生' ? '欢迎优秀应届毕业生投递。' : `要求 ${exp} 相关经验。`}`,
    publishDate: `2026-${pad2(month)}-${pad2(day)}`,
    source: 'MOCK',
    // ⚠️ 域名必须与 init.sql 种子的 mock.occupation.dev 不同，见文件头注释
    sourceUrl: `https://mock.local/job/${pad3(i)}`,
  });
}

fs.writeFileSync(OUT, JSON.stringify(jobs, null, 2) + '\n', 'utf8');

const by = (fn) => jobs.reduce((m, j) => (m[fn(j)] = (m[fn(j)] || 0) + 1, m), {});
console.log('已生成:', OUT);
console.log('职位数:', jobs.length);
console.log('行业分布:', by(j => j.industry));
console.log('城市分布:', by(j => j.city));
console.log('公司数:', new Set(jobs.map(j => j.company)).size, '/ 标题种类:', new Set(jobs.map(j => j.title)).size);
console.log('sourceUrl 样例:', jobs[0].sourceUrl, '…', jobs[jobs.length - 1].sourceUrl);
