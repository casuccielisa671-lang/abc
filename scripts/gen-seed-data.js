/**
 * init.sql 种子数据生成器（确定性，可重复运行）
 * 逻辑保证：
 *  - raw_job_data(CLEANED) 与 job_detail 一一对应（source_url 相同，raw_content 可被 DataCleanServiceImpl 解析）
 *  - crawler_log 各次 SUCCESS 的 record_count 之和 = MOCK 来源 raw_job_data 总数
 *  - analysis_result 直接由 job_detail 聚合算出（与 AnalysisJobServiceImpl 同口径：AVG((min+max)/2)、月周期）
 *  - student_behavior 按学生画像的城市/技能倾向生成，VIEW < FAVORITE < APPLY 时间有序
 *  - push_record 的推荐内容引用真实存在的职位
 *  - sys_alert 的 CRAWLER_FAILURE 与 FAILED 的 crawler_log 时间一致
 */
const fs = require('fs');
const path = require('path');

// 用法：node scripts/gen-seed-data.js（重写 init.sql 的种子数据段，改完记得重新导入数据库）
const INIT_SQL = path.join(__dirname, '..', 'occupation-common', 'src', 'main', 'resources', 'sql', 'init.sql');
const NEWS_BULK_SQL = path.join(__dirname, '..', 'occupation-common', 'src', 'main', 'resources', 'sql', 'upgrade-2026-07-13-news-bulk.sql');

// ---------- 确定性随机 ----------
function mulberry32(a) {
  return function () {
    a |= 0; a = (a + 0x6D2B79F5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const rnd = mulberry32(20260709);
const ri = (min, max) => min + Math.floor(rnd() * (max - min + 1)); // 闭区间
const pick = (arr) => arr[Math.floor(rnd() * arr.length)];
const pickN = (arr, n) => { // 不重复取 n 个
  const copy = arr.slice(); const out = [];
  for (let i = 0; i < n && copy.length; i++) out.push(copy.splice(Math.floor(rnd() * copy.length), 1)[0]);
  return out;
};

function readBulkNewsRows() {
  const sql = fs.readFileSync(NEWS_BULK_SQL, 'utf8');
  const start = sql.indexOf('(9, 1,');
  if (start < 0) {
    throw new Error('Cannot find bulk news rows in upgrade-2026-07-13-news-bulk.sql');
  }
  const end = sql.lastIndexOf(';');
  if (end < 0) {
    throw new Error('Cannot find end of bulk news rows in upgrade-2026-07-13-news-bulk.sql');
  }
  return sql.slice(start, end).trim();
}
const esc = (s) => String(s).replace(/\\/g, '\\\\').replace(/'/g, "''");
const pad = (n) => String(n).padStart(2, '0');
const dt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
const dOnly = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const mk = (y, mo, day, h = 9, mi = 0, s = 0) => new Date(y, mo - 1, day, h, mi, s);

// 与后端一致的 HALF_UP 保留 2 位
const round2 = (x) => Math.round((x + Number.EPSILON) * 100) / 100;

const HASH = '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q'; // admin123

// 开放 API 的 api_secret 同样以 BCrypt 存储（不再明文）。
// 注释里是对应的明文，第三方调用 /api/open/auth/token 时传明文。
// 换 secret 时用 BCryptPasswordEncoder().encode(明文) 生成新哈希替换即可。
const API_SECRET_HASHES = {
  demo:     '$2a$10$LotAmCrhQI8dQNSB3ouPB.NdT9mRVoQys8.5RYIaOVNnnFapmX7AC', // demo_secret_key_for_dev
  portal:   '$2a$10$7b6HPt1Iub1QutbFFTYHyOw8wzqaZ8.CfC3YegqRuWmW7C4eyaTQ6', // portal_secret_key_for_dev
  screen:   '$2a$10$fCjw5VlVieCsk5HnhZlaMe7cM0WWSt5JMTu4CZZ4xMiRdtiH75faq', // screen_secret_key_deprecated
  demoUniv: '$2a$10$PompgOywvbOs8I3NNuRhd.DJrZYoblDjF9sSoPjxxPWd.veDxughq', // demo_univ_secret_key
};

// ---------- 词表 ----------
const CITIES = ['北京', '上海', '深圳', '杭州', '广州', '成都', '武汉', '南京', '西安', '苏州'];
const CITY_W = [18, 17, 15, 12, 9, 8, 7, 6, 4, 4]; // 权重

const COMPANIES = [
  '华信云科技有限公司', '启明星辰网络技术有限公司', '云栖数智信息技术有限公司', '天翼软件股份有限公司',
  '芯联半导体科技有限公司', '慧眼视觉科技有限公司', '金桥融信金融服务有限公司', '汇通证券数据服务有限公司',
  '优选电商集团有限公司', '闪购网络科技有限公司', '知行在线教育科技有限公司', '乐游互动娱乐有限公司',
  '星辉游戏开发有限公司', '精工智造装备股份有限公司', '联达自动化技术有限公司', '康桥医疗信息科技有限公司',
  '数联云图大数据有限公司', '博睿人工智能研究院有限公司'
];

// 行业 → 职位模板（标题 / 技能池 / 薪资基线）
const INDUSTRIES = {
  '互联网/IT': {
    weight: 30,
    titles: [
      { t: 'Java开发工程师', sk: ['Java', 'Spring Boot', 'MySQL', 'Redis', '微服务'], base: 11000 },
      { t: '前端开发工程师', sk: ['JavaScript', 'Vue', 'CSS', 'TypeScript'], base: 10000 },
      { t: '后端开发工程师', sk: ['Java', 'MySQL', 'Redis', '分布式', 'Linux'], base: 11500 },
      { t: '全栈开发工程师', sk: ['JavaScript', 'Vue', 'Java', 'MySQL'], base: 12000 },
      { t: '测试开发工程师', sk: ['Python', 'Selenium', 'Linux', 'MySQL'], base: 9500 },
      { t: '运维开发工程师', sk: ['Linux', 'Docker', 'Kubernetes', 'Python'], base: 10500 },
      { t: 'Go开发工程师', sk: ['Go', 'MySQL', 'Redis', '微服务'], base: 12500 },
    ],
  },
  '人工智能': {
    weight: 16,
    titles: [
      { t: '机器学习工程师', sk: ['Python', '机器学习', 'PyTorch', 'SQL'], base: 15000 },
      { t: 'NLP算法工程师', sk: ['Python', '深度学习', 'NLP', 'PyTorch'], base: 16000 },
      { t: '计算机视觉工程师', sk: ['Python', '计算机视觉', '深度学习', 'TensorFlow'], base: 15500 },
      { t: '算法工程师', sk: ['Python', '机器学习', '数据结构', 'C++'], base: 15000 },
    ],
  },
  '大数据': {
    weight: 15,
    titles: [
      { t: '数据分析师', sk: ['SQL', 'Python', '数据分析', 'Tableau'], base: 10000 },
      { t: '大数据开发工程师', sk: ['Java', 'Spark', 'Hadoop', 'Hive'], base: 13000 },
      { t: '数据仓库工程师', sk: ['SQL', 'Hive', 'Spark', '数据建模'], base: 12500 },
      { t: '实时计算工程师', sk: ['Java', 'Flink', 'Kafka', 'Redis'], base: 14000 },
    ],
  },
  '金融': {
    weight: 10,
    titles: [
      { t: '金融数据分析师', sk: ['SQL', 'Python', '数据分析', 'Excel'], base: 11000 },
      { t: '量化研究员', sk: ['Python', 'C++', '机器学习', '数理统计'], base: 18000 },
      { t: '风控算法专员', sk: ['SQL', 'Python', '机器学习', '数据分析'], base: 12000 },
    ],
  },
  '电子商务': {
    weight: 9,
    titles: [
      { t: '电商数据运营', sk: ['数据分析', 'Excel', 'SQL', '用户运营'], base: 8500 },
      { t: '产品经理', sk: ['产品设计', 'Axure', '数据分析', '项目管理'], base: 11000 },
      { t: '用户增长运营', sk: ['用户运营', '数据分析', '文案策划'], base: 9000 },
    ],
  },
  '教育': {
    weight: 7,
    titles: [
      { t: '在线教育产品经理', sk: ['产品设计', '数据分析', '项目管理'], base: 10000 },
      { t: '课程研发工程师', sk: ['Java', 'Vue', 'MySQL'], base: 9500 },
      { t: '教学数据分析师', sk: ['SQL', 'Excel', '数据分析'], base: 8000 },
    ],
  },
  '游戏': {
    weight: 7,
    titles: [
      { t: '游戏客户端开发', sk: ['C++', 'Unity', '数据结构'], base: 13000 },
      { t: '游戏服务端开发', sk: ['Go', 'Redis', 'MySQL', '分布式'], base: 13500 },
      { t: '游戏数值策划', sk: ['Excel', '数据分析', '数理统计'], base: 9500 },
    ],
  },
  '智能制造': {
    weight: 6,
    titles: [
      { t: '嵌入式软件工程师', sk: ['C++', 'Linux', '单片机'], base: 10500 },
      { t: '工业软件开发工程师', sk: ['Java', 'MySQL', 'Linux'], base: 10000 },
      { t: '自动化测试工程师', sk: ['Python', 'Linux', 'Selenium'], base: 9000 },
    ],
  },
};

const EXPERIENCES = [
  { e: '应届生', k: 0.75, w: 22 },
  { e: '经验不限', k: 0.85, w: 18 },
  { e: '1-3年', k: 1.0, w: 30 },
  { e: '3-5年', k: 1.35, w: 20 },
  { e: '5-10年', k: 1.8, w: 10 },
];
const EDUS = [
  { e: '不限', k: 0.85, w: 10 },
  { e: '专科', k: 0.9, w: 12 },
  { e: '本科', k: 1.0, w: 58 },
  { e: '硕士', k: 1.25, w: 18 },
  { e: '博士', k: 1.6, w: 2 },
];
const weighted = (items, wKey) => {
  const total = items.reduce((s, it) => s + it[wKey], 0);
  let r = rnd() * total;
  for (const it of items) { r -= it[wKey]; if (r <= 0) return it; }
  return items[items.length - 1];
};
const pickCity = () => {
  let total = CITY_W.reduce((a, b) => a + b, 0), r = rnd() * total;
  for (let i = 0; i < CITIES.length; i++) { r -= CITY_W[i]; if (r <= 0) return CITIES[i]; }
  return CITIES[0];
};

// 城市薪资系数（一线略高）
const CITY_K = { '北京': 1.15, '上海': 1.15, '深圳': 1.1, '杭州': 1.05, '广州': 1.0, '成都': 0.9, '武汉': 0.88, '南京': 0.95, '西安': 0.85, '苏州': 0.95 };

// ---------- 采集批次（5 次成功运行）----------
const BATCHES = [
  { time: mk(2026, 6, 20, 2, 0, 0), count: 0 },
  { time: mk(2026, 6, 27, 2, 0, 0), count: 0 },
  { time: mk(2026, 7, 1, 2, 0, 0), count: 0 },
  { time: mk(2026, 7, 5, 2, 0, 0), count: 0 },
  { time: mk(2026, 7, 8, 2, 0, 0), count: 0 },
];

// ---------- 生成职位 ----------
// 月份分布（发布量逐月上升，趋势图好看且符合春招→夏招节奏）
const MONTH_PLAN = [ // [月, MOCK职位数]
  [2, 12], [3, 18], [4, 24], [5, 30], [6, 36], [7, 42],
];
const jobs = [];        // {id,title,company,city,industry,salMin,salMax,edu,exp,skills,desc,publishDate,source,sourceUrl,createTime}
let jobId = 0;

function makeSalary(base, expK, eduK, cityK) {
  const mid = base * expK * eduK * cityK * (0.9 + rnd() * 0.2);
  let min = Math.round(mid * 0.85 / 500) * 500;
  let max = Math.round(mid * 1.25 / 500) * 500;
  if (max <= min) max = min + 2000;
  return [min, max];
}

function makeJob(pubDate, source) {
  jobId++;
  const indName = weighted(Object.entries(INDUSTRIES).map(([k, v]) => ({ k, ...v })), 'weight').k;
  const ind = INDUSTRIES[indName];
  const tpl = pick(ind.titles);
  const exp = weighted(EXPERIENCES, 'w');
  const edu = weighted(EDUS, 'w');
  const city = pickCity();
  const [salMin, salMax] = makeSalary(tpl.base, exp.k, edu.k, CITY_K[city]);
  // 技能：模板技能全取 + 30% 概率附加 1 个通用技能
  const skills = tpl.sk.slice();
  if (rnd() < 0.3) {
    const extra = pick(['Git', 'Linux', '沟通能力', '团队协作']);
    if (!skills.includes(extra)) skills.push(extra);
  }
  const company = pick(COMPANIES);
  const desc = `【岗位职责】负责${indName}方向${tpl.t}相关工作，参与需求分析、方案设计与落地实施。` +
    `【任职要求】${edu.e === '不限' ? '学历不限' : edu.e + '及以上学历'}，${exp.e === '经验不限' ? '经验不限' : '工作经验' + exp.e}；` +
    `熟悉 ${skills.slice(0, 3).join('、')} 等技术/工具；具备良好的学习能力与团队协作意识。`;
  return {
    id: jobId, title: tpl.t, company, city, industry: indName,
    salMin, salMax, edu: edu.e, exp: exp.e, skills, desc,
    publishDate: pubDate, source, sourceUrl: null, createTime: null,
  };
}

// MOCK 职位（走采集链路）
for (const [mo, cnt] of MONTH_PLAN) {
  for (let i = 0; i < cnt; i++) {
    const day = mo === 7 ? ri(1, 8) : ri(1, 28);
    const job = makeJob(mk(2026, mo, day), 'MOCK');
    // 采集批次：只能被发布日之后的批次采到
    const valid = BATCHES.filter(b => b.time >= job.publishDate);
    const batch = valid.length ? pick(valid) : BATCHES[BATCHES.length - 1];
    batch.count++;
    const offset = batch.count * 37; // 批内错开秒数
    job.fetchTime = new Date(batch.time.getTime() + offset * 1000);
    job.createTime = new Date(job.fetchTime.getTime() + ri(5, 50) * 1000);
    job.sourceUrl = `https://mock.occupation.dev/jobs/${job.id}`;
    jobs.push(job);
  }
}
const mockJobCount = jobs.length;

// HR 发布职位（两家企业，对应 hr / hr01 / hr02 的公司）
// pub = 发布者 user_id，写入 job_detail.publisher_id，供 HR 端「只看我发布的」过滤：
//   hr(4)=2 个、hr01(19)=1 个、hr02(20)=3 个，三个账号登录后看到的列表各不相同
// 站内发布的职位 —— 这些才是学生能真正投递的岗位。
// 采集来的职位在平台上没有主人，学生投了没人处理（见 RecommendController.apply 的守卫）。
// 所以站内职位不能太少，否则学生首页的「可投递岗位」栏几乎是空的。
//   3 家公司 × 6 个 HR，共 24 个职位，每个 HR 3~5 个。
const HR_COMPANIES = [
  '云聘互联科技有限公司',       // 0 → hr(4), hr03(26)
  '智汇未来教育科技有限公司',   // 1 → hr02(20), hr04(27)
  '恒信数据技术有限公司',       // 2 → hr01(19), hr05(28)
];
const HR_JOB_SPECS = [
  // ---- 云聘互联（互联网/大数据）：hr=4, hr03=26 ----
  { t: 'Java开发工程师（校招）', ind: '互联网/IT', sk: ['Java', 'Spring Boot', 'MySQL'], base: 9000, c: 0, pub: 4, city: '杭州', exp: '应届生', edu: '本科' },
  { t: '大数据平台研发工程师', ind: '大数据', sk: ['Java', 'Spark', 'Kafka', 'Hive'], base: 14000, c: 0, pub: 4, city: '上海', exp: '3-5年', edu: '本科' },
  { t: '后端开发工程师（微服务方向）', ind: '互联网/IT', sk: ['Java', '微服务', 'Redis', 'Docker'], base: 12000, c: 0, pub: 4, city: '杭州', exp: '1-3年', edu: '本科' },
  { t: '测试开发工程师', ind: '互联网/IT', sk: ['Python', 'Selenium', 'Linux'], base: 9000, c: 0, pub: 4, city: '杭州', exp: '1-3年', edu: '本科' },
  { t: '云平台运维工程师', ind: '互联网/IT', sk: ['Linux', 'Docker', 'Kubernetes'], base: 11000, c: 0, pub: 26, city: '深圳', exp: '1-3年', edu: '本科' },
  { t: '数据仓库开发（校招）', ind: '大数据', sk: ['SQL', 'Hive', 'Spark'], base: 10000, c: 0, pub: 26, city: '上海', exp: '应届生', edu: '本科' },
  { t: '实时计算工程师', ind: '大数据', sk: ['Java', 'Kafka', '分布式'], base: 15000, c: 0, pub: 26, city: '北京', exp: '3-5年', edu: '硕士' },
  { t: '前端开发实习生', ind: '互联网/IT', sk: ['JavaScript', 'Vue', 'CSS'], base: 5000, c: 0, pub: 26, city: '杭州', exp: '应届生', edu: '不限' },

  // ---- 智汇未来教育（教育/人工智能）：hr02=20, hr04=27 ----
  { t: '在线课程产品经理', ind: '教育', sk: ['产品设计', '数据分析', '项目管理'], base: 10000, c: 1, pub: 20, city: '北京', exp: '1-3年', edu: '本科' },
  { t: '教学数据分析师（校招）', ind: '教育', sk: ['SQL', 'Python', '数据分析'], base: 8000, c: 1, pub: 20, city: '北京', exp: '应届生', edu: '本科' },
  { t: 'AI课程算法工程师', ind: '人工智能', sk: ['Python', '机器学习', 'NLP'], base: 15000, c: 1, pub: 20, city: '北京', exp: '1-3年', edu: '硕士' },
  { t: '课程研发工程师（Java方向）', ind: '教育', sk: ['Java', 'Spring Boot', 'MySQL'], base: 11000, c: 1, pub: 20, city: '广州', exp: '1-3年', edu: '本科' },
  { t: '智能推荐算法实习生', ind: '人工智能', sk: ['Python', '机器学习', '深度学习'], base: 6000, c: 1, pub: 27, city: '北京', exp: '应届生', edu: '硕士' },
  { t: '教育数据挖掘工程师', ind: '人工智能', sk: ['Python', '数据分析', '机器学习'], base: 13000, c: 1, pub: 27, city: '上海', exp: '1-3年', edu: '硕士' },
  { t: '前端工程师（教学平台）', ind: '教育', sk: ['JavaScript', 'Vue', 'CSS', 'Git'], base: 9000, c: 1, pub: 27, city: '成都', exp: '1-3年', edu: '专科' },
  { t: '用户运营专员（校招）', ind: '教育', sk: ['用户运营', '数据分析', 'Excel'], base: 6500, c: 1, pub: 27, city: '杭州', exp: '应届生', edu: '本科' },

  // ---- 恒信数据（金融/安全/智能制造）：hr01=19, hr05=28 ----
  { t: '金融数据分析师（校招）', ind: '金融', sk: ['SQL', 'Python', '数理统计', '数据分析'], base: 11000, c: 2, pub: 19, city: '上海', exp: '应届生', edu: '硕士' },
  { t: '风控建模工程师', ind: '金融', sk: ['Python', '机器学习', 'SQL'], base: 16000, c: 2, pub: 19, city: '上海', exp: '3-5年', edu: '硕士' },
  { t: '信息安全工程师', ind: '互联网/IT', sk: ['Linux', 'Python', '网络安全'], base: 13000, c: 2, pub: 19, city: '深圳', exp: '1-3年', edu: '本科' },
  { t: '安全运营实习生', ind: '互联网/IT', sk: ['Linux', '网络安全', 'Docker'], base: 5500, c: 2, pub: 19, city: '深圳', exp: '应届生', edu: '本科' },
  { t: '嵌入式软件工程师', ind: '智能制造', sk: ['C++', '单片机', 'Linux'], base: 10000, c: 2, pub: 28, city: '苏州', exp: '1-3年', edu: '本科' },
  { t: '物联网平台开发', ind: '智能制造', sk: ['C++', 'Linux', 'Python'], base: 12000, c: 2, pub: 28, city: '苏州', exp: '1-3年', edu: '本科' },
  { t: '量化研究员（校招）', ind: '金融', sk: ['Python', '数理统计', '数据分析'], base: 18000, c: 2, pub: 28, city: '北京', exp: '应届生', edu: '硕士' },
  { t: '前端开发工程师（数据可视化）', ind: '金融', sk: ['JavaScript', 'Vue', 'CSS'], base: 12000, c: 2, pub: 28, city: '南京', exp: '1-3年', edu: '本科' },
];
for (const s of HR_JOB_SPECS) {
  jobId++;
  const mo = ri(6, 7);
  const pub = mk(2026, mo, mo === 7 ? ri(1, 8) : ri(15, 30), 10, 30);
  const mid = s.base * (0.95 + rnd() * 0.1);
  const min = Math.round(mid * 0.9 / 500) * 500, max = Math.round(mid * 1.3 / 500) * 500;
  jobs.push({
    id: jobId, title: s.t, company: HR_COMPANIES[s.c], city: s.city, industry: s.ind,
    salMin: min, salMax: max, edu: s.edu, exp: s.exp, skills: s.sk,
    desc: `【企业直招】${HR_COMPANIES[s.c]}招聘${s.t}，工作地点${s.city}。要求掌握 ${s.sk.join('、')}，${s.edu === '不限' ? '学历不限' : s.edu + '及以上学历'}。通过本平台发布，欢迎在校学生投递。`,
    publishDate: pub, source: 'HR_PUBLISH', sourceUrl: null, publisherId: s.pub,
    createTime: new Date(pub.getTime() + 9 * 3600 * 1000),
  });
}

// ---------- 学生与画像 ----------
const STUDENT_DEFS = [
  // [id, username, 姓名, 专业, 技能, 城市, 行业, 薪min, 薪max, 学历]
  [2, 'student', '演示学生', '计算机科学与技术', ['Java', 'Spring Boot', 'MySQL', 'Redis', 'Linux', 'Git'], '杭州', '互联网/IT', 8000, 15000, '本科'],
  [5, 'student01', '陈嘉怡', '软件工程', ['Java', 'MySQL', 'Vue', 'JavaScript', 'Git'], '上海', '互联网/IT', 8000, 14000, '本科'],
  [6, 'student02', '李昊然', '数据科学与大数据技术', ['Python', 'SQL', 'Spark', 'Hadoop', '数据分析'], '北京', '大数据', 10000, 18000, '本科'],
  [7, 'student03', '王雨桐', '人工智能', ['Python', '机器学习', '深度学习', 'PyTorch', 'SQL'], '北京', '人工智能', 12000, 20000, '硕士'],
  [8, 'student04', '张子墨', '计算机科学与技术', ['C++', '数据结构', 'Linux', 'Python'], '深圳', '游戏', 10000, 16000, '本科'],
  [9, 'student05', '刘思远', '信息安全', ['Linux', 'Python', '网络安全', 'Docker'], '深圳', '互联网/IT', 9000, 15000, '本科'],
  [10, 'student06', '杨欣然', '电子商务', ['数据分析', 'Excel', 'SQL', '用户运营', '文案策划'], '杭州', '电子商务', 6000, 10000, '本科'],
  [11, 'student07', '赵一鸣', '软件工程', ['Java', 'Spring Boot', '微服务', 'Redis', 'Kafka', 'Docker'], '南京', '互联网/IT', 9000, 16000, '本科'],
  [12, 'student08', '黄诗涵', '统计学', ['SQL', 'Python', '数理统计', '数据分析', 'Tableau'], '上海', '金融', 9000, 15000, '硕士'],
  [13, 'student09', '周俊杰', '计算机应用技术', ['JavaScript', 'Vue', 'CSS', 'Git'], '成都', '互联网/IT', 6000, 10000, '专科'],
  [14, 'student10', '吴雅静', '教育技术学', ['产品设计', '数据分析', '项目管理', 'Axure'], '北京', '教育', 7000, 12000, '本科'],
  [15, 'student11', '徐浩宇', '物联网工程', ['C++', '单片机', 'Linux', 'Python'], '苏州', '智能制造', 7000, 12000, '本科'],
  // student12(id16) 故意不建画像 → 测试“请先完善个人画像”提示
];

// ---------- 生成行为（画像驱动，逻辑自洽）----------
//
// 【行为与职位归属的硬约束，必须遵守，否则会造出后端拒绝的非法数据】
//   APPLY   只能落在 HR_PUBLISH 职位上（有 publisher_id）。采集职位在平台上没有主人，
//           后端的 RecommendController.apply() 会直接拒绝。
//   CONTACT 只能落在采集职位上。站内职位应该走投递，后端同样会拒绝。
//   VIEW / FAVORITE / IGNORE 两类职位都可以。
//
const behaviors = []; // {tenantId,userId,jobId,action,time}
const isPlatformJob = (j) => j.source === 'HR_PUBLISH';

function genBehaviorsFor(userId, tenantId, def) {
  const mySkills = def[4], myCity = def[5];
  // 打分选出该学生“感兴趣”的职位
  const scored = jobs.map(j => {
    let s = 0;
    if (j.city === myCity) s += 3;
    s += j.skills.filter(k => mySkills.includes(k)).length;
    return { j, s: s + rnd() * 2 };
  }).sort((a, b) => b.s - a.s);
  const viewCount = ri(10, 16);
  const viewed = scored.slice(0, viewCount).map(x => x.j);

  for (const job of viewed) {
    // 行为时间必须在职位入库之后
    const minTime = Math.max(job.createTime.getTime(), mk(2026, 6, 21).getTime());
    const span = mk(2026, 7, 9, 12).getTime() - minTime;
    if (span <= 0) continue;
    const viewT = new Date(minTime + rnd() * span * 0.6);
    behaviors.push({ tenantId, userId, jobId: job.id, action: 'VIEW', time: viewT });

    const r = rnd();
    if (r < 0.35) {
      // 收藏 → 有一定概率转化
      const favT = new Date(viewT.getTime() + ri(60, 86400) * 1000);
      behaviors.push({ tenantId, userId, jobId: job.id, action: 'FAVORITE', time: favT });
      if (rnd() < 0.45) {
        const act = isPlatformJob(job) ? 'APPLY' : 'CONTACT';
        behaviors.push({ tenantId, userId, jobId: job.id, action: act, time: new Date(favT.getTime() + ri(3600, 172800) * 1000) });
      }
    } else if (r < 0.45) {
      // 直接转化：站内投递 / 外部自主联系
      const act = isPlatformJob(job) ? 'APPLY' : 'CONTACT';
      behaviors.push({ tenantId, userId, jobId: job.id, action: act, time: new Date(viewT.getTime() + ri(300, 86400) * 1000) });
    } else if (r > 0.9) {
      behaviors.push({ tenantId, userId, jobId: job.id, action: 'IGNORE', time: new Date(viewT.getTime() + ri(30, 600) * 1000) });
    }
  }
}
for (const def of STUDENT_DEFS) genBehaviorsFor(def[0], 1, def);
// 租户2的学生（共享职位池）
const T2_STUDENT = [24, 'student', '示范学生', '软件工程', ['Java', 'MySQL', 'Vue'], '武汉', '互联网/IT', 7000, 12000, '本科'];
genBehaviorsFor(24, 2, T2_STUDENT);

// ---------- 保证每个 HR 职位都收到投递 ----------
// 上面的打分是从全部职位里挑，站内职位只占 24/114，概率上会有职位一条投递都收不到，
// 结果是某些 HR 登录后「收到的投递」是空的，双边闭环看不出效果。
// 这里为每个 HR 职位补足投递：按「技能重合 + 城市一致」挑最匹配的学生，与推荐逻辑自洽。
const APPLIES_PER_HR_JOB = 4;
function hasBehavior(userId, jobId, action) {
  return behaviors.some(b => b.userId === userId && b.jobId === jobId && b.action === action);
}
for (const job of jobs.filter(isPlatformJob)) {
  const ranked = STUDENT_DEFS
    .map(d => ({
      id: d[0],
      score: job.skills.filter(k => d[4].includes(k)).length + (d[5] === job.city ? 1 : 0),
    }))
    .sort((a, b) => b.score - a.score || a.id - b.id)
    .slice(0, APPLIES_PER_HR_JOB);

  for (const { id } of ranked) {
    const base = Math.max(job.createTime.getTime(), mk(2026, 6, 21).getTime());
    const span = mk(2026, 7, 9, 12).getTime() - base;
    if (span <= 0) continue;
    const viewT = new Date(base + rnd() * span * 0.5);
    if (!hasBehavior(id, job.id, 'VIEW')) {
      behaviors.push({ tenantId: 1, userId: id, jobId: job.id, action: 'VIEW', time: viewT });
    }
    if (!hasBehavior(id, job.id, 'APPLY')) {
      behaviors.push({ tenantId: 1, userId: id, jobId: job.id, action: 'APPLY', time: new Date(viewT.getTime() + ri(600, 86400) * 1000) });
    }
  }
}

// student12(id 16) 没有画像也没有简历，但投了一个站内职位 ——
// 用来测 HR 端「未完善画像 / 未填写简历」的空态展示
{
  const job = jobs.filter(isPlatformJob)[0];
  const viewT = mk(2026, 7, 6, 15, 20);
  behaviors.push({ tenantId: 1, userId: 16, jobId: job.id, action: 'VIEW', time: viewT });
  behaviors.push({ tenantId: 1, userId: 16, jobId: job.id, action: 'APPLY', time: new Date(viewT.getTime() + 7200 * 1000) });
}

// 自检：APPLY 只落站内、CONTACT 只落采集。违反就直接炸，别让脏数据流进 init.sql
{
  const jobById = new Map(jobs.map(j => [j.id, j]));
  for (const b of behaviors) {
    const j = jobById.get(b.jobId);
    if (b.action === 'APPLY' && !isPlatformJob(j)) {
      throw new Error(`非法种子数据：APPLY 落在采集职位 ${b.jobId} 上`);
    }
    if (b.action === 'CONTACT' && isPlatformJob(j)) {
      throw new Error(`非法种子数据：CONTACT 落在站内职位 ${b.jobId} 上`);
    }
  }
}

behaviors.sort((a, b) => a.time - b.time);
// 行为时间截断到 2026-07-09 12:00 之前
const behaviorsFinal = behaviors.filter(b => b.time <= mk(2026, 7, 9, 12));

// ---------- 投递记录（业务实体，由 APPLY 行为派生）----------
// student_behavior 里的 APPLY 是不可变的行为埋点；job_application 是可流转的业务实体。
// 同一次投递的两个视角，后端在 apply() 里双写。这里按同样的规则生成，保证一一对应。
//
// 状态刻意铺开五种：HR 一登录就能看到完整的处理进度分布，管理端「就业分析」的
// 转化漏斗也才有形状，而不是一根光秃秃的「全部待查看」。
const jobById = new Map(jobs.map(j => [j.id, j]));
const STATUS_PLAN = [
  { s: 'SUBMITTED', w: 0.34, note: null },
  { s: 'VIEWED', w: 0.22, note: null },
  { s: 'INTERVIEW', w: 0.20, note: '简历匹配度高，约技术一面' },
  { s: 'OFFER', w: 0.10, note: '综合表现优秀，已发放录用意向' },
  { s: 'REJECTED', w: 0.14, note: '岗位要求的项目经验暂不匹配' },
];
function pickStatus() {
  let r = rnd(), acc = 0;
  for (const p of STATUS_PLAN) {
    acc += p.w;
    if (r < acc) return p;
  }
  return STATUS_PLAN[STATUS_PLAN.length - 1];
}
const applications = behaviorsFinal
  .filter(b => b.action === 'APPLY')
  .map(b => {
    const p = pickStatus();
    // 状态变更时间：投递后 2 小时 ~ 10 天，且不晚于数据截止时刻
    const changedAt = p.s === 'SUBMITTED'
      ? null
      : new Date(Math.min(b.time.getTime() + ri(7200, 864000) * 1000, mk(2026, 7, 9, 11, 30).getTime()));
    return {
      tenantId: b.tenantId, userId: b.userId, jobId: b.jobId,
      publisherId: jobById.get(b.jobId).publisherId,
      status: p.s, note: p.note, appliedAt: b.time, changedAt,
    };
  });

// ---------- 推送（引用真实职位）----------
const pushes = [];
let pushTimeBase = mk(2026, 7, 1, 8, 30).getTime();
for (const def of STUDENT_DEFS) {
  const uid = def[0], myCity = def[5], mySkills = def[4];
  const match = jobs.filter(j => j.city === myCity && j.skills.some(k => mySkills.includes(k)));
  const recos = pickN(match.length ? match : jobs, Math.min(2, match.length || 1));
  for (const j of recos) {
    pushes.push({
      tenantId: 1, userId: uid, type: 'RECOMMEND',
      title: `为你推荐：${j.title}`,
      content: `根据你的画像为你匹配到职位【${j.title}】- ${j.company}（${j.city}，${j.salMin}-${j.salMax}元/月），快去看看吧！`,
      isRead: rnd() < 0.5 ? 1 : 0,
      time: new Date(pushTimeBase += ri(3600, 20000) * 1000),
    });
  }
  pushes.push({
    tenantId: 1, userId: uid, type: 'SYSTEM',
    title: '欢迎使用职业能力大数据服务平台',
    content: '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。',
    isRead: 1, time: mk(2026, 6, 20, 10, 0, def[0]),
  });
}
// 未完善画像的 student12 只有系统提醒
pushes.push({
  tenantId: 1, userId: 16, type: 'SYSTEM', title: '请完善个人画像',
  content: '检测到你尚未填写专业、技能与求职意向，完善后才能使用职位推荐功能。',
  isRead: 0, time: mk(2026, 7, 2, 9, 0, 0),
});
pushes.push({
  tenantId: 2, userId: 24, type: 'SYSTEM', title: '欢迎使用职业能力大数据服务平台',
  content: '完善个人画像后，系统将为你提供更精准的职位推荐。', isRead: 0, time: mk(2026, 7, 3, 9, 0, 0),
});

// ---------- 分析结果（与 AnalysisJobServiceImpl 同口径）----------
function aggregate(keyFn) {
  const m = new Map();
  for (const j of jobs) {
    const k = keyFn(j);
    if (!k) continue;
    const e = m.get(k) || { n: 0, sum2: 0 };
    e.n++; e.sum2 += j.salMin + j.salMax;
    m.set(k, e);
  }
  return m;
}
const CALC_TIME = mk(2026, 7, 9, 8, 0, 0);
const analysisRows = []; // {dimension,value,metric,val,periodValue}
function pushAgg(dim, map) {
  for (const [k, e] of map) {
    analysisRows.push({ dim, value: k, metric: 'job_count', val: e.n, period: '2026-07' });
    analysisRows.push({ dim, value: k, metric: 'avg_salary', val: round2(e.sum2 / (2 * e.n)), period: '2026-07' });
  }
}
pushAgg('industry', aggregate(j => j.industry));
pushAgg('city', aggregate(j => j.city));
pushAgg('education', aggregate(j => j.edu));
// skill：仅 job_count
{
  const freq = new Map();
  for (const j of jobs) for (const s of j.skills) freq.set(s, (freq.get(s) || 0) + 1);
  const top = [...freq.entries()].sort((a, b) => b[1] - a[1]).slice(0, 100);
  for (const [k, n] of top) analysisRows.push({ dim: 'skill', value: k, metric: 'job_count', val: n, period: '2026-07' });
}
// trend：按发布月
{
  const m = new Map();
  for (const j of jobs) {
    const k = `${j.publishDate.getFullYear()}-${pad(j.publishDate.getMonth() + 1)}`;
    const e = m.get(k) || { n: 0, sum2: 0 };
    e.n++; e.sum2 += j.salMin + j.salMax;
    m.set(k, e);
  }
  for (const [k, e] of [...m.entries()].sort()) {
    analysisRows.push({ dim: 'trend', value: k, metric: 'job_count', val: e.n, period: k });
    analysisRows.push({ dim: 'trend', value: k, metric: 'avg_salary', val: round2(e.sum2 / (2 * e.n)), period: k });
  }
}

// ---------- 组装 SQL ----------
const L = [];
L.push('-- ============================================================');
L.push('-- 初始化种子数据（由 scripts/gen-seed-data.js 确定性生成，勿手改）');
L.push('--');
L.push('-- 【职位数据的三个来源，看 source + publisher_id 两列就能区分】');
L.push('--   source=MOCK,       publisher_id=NULL → 采集来的（无主，不可站内投递）');
L.push('--   source=HR_PUBLISH, publisher_id=NOT NULL → HR 在平台上发布的（可投递）');
L.push('--   source=OFFICIAL_PUBLIC, publisher_id=NULL → 官方公开招聘公告采来的（无主）');
L.push('--');
L.push('-- 【为什么种子里的 90 个职位 source 也写着 MOCK？】');
L.push('--   它们模拟「系统已经运行了一段时间、采集任务跑过 5 次」的历史状态，');
L.push('--   所以配了完整的证据链：90 条 raw_job_data 归档 + crawler_log 五次成功记录。');
L.push('--   这些职位是 init.sql 直接 INSERT 的，从没真的经过爬虫 —— 但在数据模型上，');
L.push('--   它们与「真的采集进来的职位」不可区分，这是有意为之（否则看板没有历史数据）。');
L.push('--   ⚠️ 它们的 source_url 用 mock.occupation.dev 域名，而运行时 Mock 采集用的是');
L.push('--   mock.local（见 mock/mock-jobs.json）。两个域名必须不同：清洗按 source_url 去重，');
L.push('--   撞了的话点「启动采集」一条都进不来。');
L.push('--');
L.push('-- 【生成器维护的不变量，改动时务必保持】');
L.push('--   raw_job_data ↔ job_detail 按 source_url 一一对应；');
L.push('--   crawler_log 成功批次 record_count 合计 = MOCK 原始数据条数；');
L.push('--   analysis_result 由 job_detail 按后端同口径聚合得出；');
L.push('--   student_behavior / push_record 引用真实存在的用户与职位');
L.push('--');
L.push('-- 所有账号密码均为 admin123');
L.push('-- ============================================================');
L.push('');
L.push('-- ---------- 租户 ----------');
L.push("INSERT INTO sys_tenant (id, name, status) VALUES");
L.push("(1, '测试学院', 1),");
L.push("(2, '示范大学', 1),");
L.push("(3, '停用学院', 0);  -- 已禁用租户：测试“租户停用后无法登录”");
L.push('');

// ---------- 班级（学院内组织：专业-入学年级-班级）----------
// 按现有学生的专业建；入学年级混用 2022/2023，以演示"届老师"的年级筛选有对比
const CLASSES = [
  // [id, tenantId, major, enrollYear, className]
  [1, 1, '软件工程', 2022, '1班'],
  [2, 1, '计算机科学与技术', 2022, '1班'],
  [3, 1, '数据科学与大数据技术', 2022, '1班'],
  [4, 1, '人工智能', 2022, '1班'],
  [5, 1, '信息安全', 2022, '1班'],
  [6, 1, '统计学', 2022, '1班'],
  [7, 1, '物联网工程', 2022, '1班'],
  [8, 1, '电子商务', 2023, '1班'],
  [9, 1, '计算机应用技术', 2023, '1班'],
  [10, 1, '教育技术学', 2023, '1班'],
  [11, 2, '软件工程', 2022, '1班'],       // 租户2 示范大学
];
const classCode = (c) => `${c[2]}-${c[3]}-${c[4]}`;
L.push('-- ---------- 班级（学院内组织：专业-入学年级-班级）----------');
L.push('INSERT INTO sys_class (id, tenant_id, major, enroll_year, class_name, code, status) VALUES');
L.push(CLASSES.map(c =>
  `(${c[0]}, ${c[1]}, '${esc(c[2])}', ${c[3]}, '${esc(c[4])}', '${esc(classCode(c))}', 1)`
).join(',\n') + ';');
L.push('');

// 学生 user id → 班级 id（班级归属放 sys_user，不依赖选填的画像）
// student12(16) 无画像但有班级；student98/99(禁用/删除) 故意不入班（class_id=NULL）
const STUDENT_CLASS = {
  2: 2, 8: 2,           // 计算机科学与技术-2022-1班：演示学生、student04
  5: 1, 11: 1, 16: 1,   // 软件工程-2022-1班：student01、student07、student12(无画像)
  6: 3,                 // 数据科学-2022
  7: 4,                 // 人工智能-2022
  9: 5,                 // 信息安全-2022
  12: 6,                // 统计学-2022（student08）
  15: 7,                // 物联网-2022（student11）
  10: 8,                // 电子商务-2023（student06）
  13: 9,                // 计算机应用-2023（student09）
  14: 10,               // 教育技术-2023（student10）
  24: 11,               // 租户2 示范学生
};

L.push('-- ---------- 用户（密码均 admin123）----------');
const users = [
  [1, 1, 'admin', 'ADMIN', '系统管理员', '13800000001', 'admin@test.edu.cn', 1, 0],
  [2, 1, 'student', 'STUDENT', '演示学生', '13800000002', 'student@stu.test.edu.cn', 1, 0],
  [3, 1, 'teacher', 'TEACHER', '演示教师', '13800000003', 'teacher@test.edu.cn', 1, 0],
  [4, 1, 'hr', 'HR', '演示HR', '13800000004', 'hr@yunpin.example.com', 1, 0],
  [5, 1, 'student01', 'STUDENT', '陈嘉怡', '13811000001', 'chenjiayi@stu.test.edu.cn', 1, 0],
  [6, 1, 'student02', 'STUDENT', '李昊然', '13811000002', 'lihaoran@stu.test.edu.cn', 1, 0],
  [7, 1, 'student03', 'STUDENT', '王雨桐', '13811000003', 'wangyutong@stu.test.edu.cn', 1, 0],
  [8, 1, 'student04', 'STUDENT', '张子墨', '13811000004', 'zhangzimo@stu.test.edu.cn', 1, 0],
  [9, 1, 'student05', 'STUDENT', '刘思远', '13811000005', 'liusiyuan@stu.test.edu.cn', 1, 0],
  [10, 1, 'student06', 'STUDENT', '杨欣然', '13811000006', 'yangxinran@stu.test.edu.cn', 1, 0],
  [11, 1, 'student07', 'STUDENT', '赵一鸣', '13811000007', 'zhaoyiming@stu.test.edu.cn', 1, 0],
  [12, 1, 'student08', 'STUDENT', '黄诗涵', '13811000008', 'huangshihan@stu.test.edu.cn', 1, 0],
  [13, 1, 'student09', 'STUDENT', '周俊杰', '13811000009', 'zhoujunjie@stu.test.edu.cn', 1, 0],
  [14, 1, 'student10', 'STUDENT', '吴雅静', '13811000010', 'wuyajing@stu.test.edu.cn', 1, 0],
  [15, 1, 'student11', 'STUDENT', '徐浩宇', '13811000011', 'xuhaoyu@stu.test.edu.cn', 1, 0],
  [16, 1, 'student12', 'STUDENT', '孙梦琪', '13811000012', 'sunmengqi@stu.test.edu.cn', 1, 0], // 无画像
  [17, 1, 'teacher01', 'TEACHER', '王建国', '13822000001', 'wangjianguo@test.edu.cn', 1, 0],
  [18, 1, 'teacher02', 'TEACHER', '林晓芳', '13822000002', 'linxiaofang@test.edu.cn', 1, 0],
  [19, 1, 'hr01', 'HR', '郑倩', '13833000001', 'zhengqian@yunpin.example.com', 1, 0],
  [20, 1, 'hr02', 'HR', '高翔', '13833000002', 'gaoxiang@zhihui.example.com', 1, 0],
  [21, 1, 'student98', 'STUDENT', '钱多多（已禁用）', '13844000001', 'qianduoduo@stu.test.edu.cn', 0, 0], // 禁用账号
  [22, 1, 'student99', 'STUDENT', '孔乙己（已删除）', '13844000002', 'kongyiji@stu.test.edu.cn', 1, 1],   // 逻辑删除
  [23, 2, 'admin', 'ADMIN', '示范大学管理员', '13900000001', 'admin@demo.edu.cn', 1, 0],
  [24, 2, 'student', 'STUDENT', '示范学生', '13900000002', 'student@demo.edu.cn', 1, 0],
  [25, 2, 'teacher', 'TEACHER', '示范教师', '13900000003', 'teacher@demo.edu.cn', 1, 0],
  // 后补的 HR：站内职位扩到 24 个后，6 个 HR 才够分（每人 3~5 个）。
  // id 从 26 起，不打乱租户 2 已占的 23~25
  [26, 1, 'hr03', 'HR', '孙浩', '13833000003', 'sunhao@yunpin.example.com', 1, 0],
  [27, 1, 'hr04', 'HR', '许静怡', '13833000004', 'xujingyi@zhihui.example.com', 1, 0],
  [28, 1, 'hr05', 'HR', '罗晨', '13833000005', 'luochen@hengxin.example.com', 1, 0],
];
L.push('INSERT INTO sys_user (id, tenant_id, username, password_hash, role, real_name, phone, email, status, deleted, class_id) VALUES');
L.push(users.map(u =>
  `(${u[0]}, ${u[1]}, '${u[2]}', '${HASH}', '${u[3]}', '${esc(u[4])}', '${u[5]}', '${u[6]}', ${u[7]}, ${u[8]}, ${STUDENT_CLASS[u[0]] || 'NULL'})`
).join(',\n') + ';');
L.push('');

// ---------- 教师可见范围（班主任=CLASS / 专业老师=MAJOR / 届老师=GRADE）----------
// 三种范围各一，可见学生互不重叠，一眼看出区别
const TEACHER_SCOPES = [
  // [id, tenantId, teacherId, scopeType, scopeValue]
  [1, 1, 17, 'CLASS', '1'],               // teacher01 王建国 → 班主任 软件工程-2022-1班（student01/07/12）
  [2, 1, 18, 'MAJOR', '计算机科学与技术'],  // teacher02 林晓芳 → 专业老师（演示学生、student04）
  [3, 1, 3,  'GRADE', '2022'],             // teacher 演示教师 → 届老师 2022 级（全部 2022 级学生）
  [4, 2, 25, 'CLASS', '11'],               // 租户2 演示教师 → 班主任 软件工程-2022-1班
];
L.push('-- ---------- 教师可见范围（班主任/专业老师/届老师）----------');
L.push('INSERT INTO teacher_scope (id, tenant_id, teacher_id, scope_type, scope_value) VALUES');
L.push(TEACHER_SCOPES.map(s =>
  `(${s[0]}, ${s[1]}, ${s[2]}, '${s[3]}', '${esc(s[4])}')`
).join(',\n') + ';');
L.push('');
L.push('-- ---------- 学生画像（student12/孙梦琪 故意无画像，测试“请先完善画像”提示）----------');
const profileRows = [];
let profId = 0;
for (const d of STUDENT_DEFS) {
  profId++;
  profileRows.push(`(${profId}, 1, ${d[0]}, '${esc(d[3])}', '${esc(JSON.stringify(d[4]))}', '${d[5]}', '${esc(d[6])}', ${d[7]}, ${d[8]}, '${d[9]}')`);
}
profId++;
profileRows.push(`(${profId}, 2, 24, '${esc(T2_STUDENT[3])}', '${esc(JSON.stringify(T2_STUDENT[4]))}', '${T2_STUDENT[5]}', '${esc(T2_STUDENT[6])}', ${T2_STUDENT[7]}, ${T2_STUDENT[8]}, '${T2_STUDENT[9]}')`);
L.push('INSERT INTO sys_student_profile (id, tenant_id, user_id, major, skills, expected_city, expected_industry, expected_salary_min, expected_salary_max, education_level) VALUES');
L.push(profileRows.join(',\n') + ';');
L.push('');

// ---------- 学生简历 ----------
// 13 个有画像的学生全部建简历，HR 点开任何一条投递都能看到内容。
// 「未填写简历」的空态由 student12(id 16) 覆盖 —— 他没画像、没简历，却投了一个站内职位。
const RESUME_USER_IDS = STUDENT_DEFS.map(d => d[0]);
const INTENTION_BY_MAJOR = {
  '计算机科学与技术': 'Java后端开发工程师',
  '软件工程': '后端开发工程师',
  '数据科学与大数据技术': '大数据开发工程师',
  '人工智能': '算法工程师',
  '信息安全': '安全工程师',
  '电子商务': '数据分析师',
  '统计学': '数据分析师',
  '计算机应用技术': '前端开发工程师',
  '教育技术学': '产品经理',
  '物联网工程': '嵌入式开发工程师',
};
const HONOR_POOL = [
  '国家励志奖学金', '校级一等奖学金', 'ACM-ICPC 省赛二等奖', '蓝桥杯省赛一等奖',
  '全国大学生数学建模竞赛省二等奖', 'CET-6（546 分）', '软件设计师（中级）',
  '“互联网+”大学生创新创业大赛校赛金奖', '优秀学生干部',
];
const userById = new Map(users.map(u => [u[0], u]));
const resumeRows = [];
let resumeId = 0;
for (const d of STUDENT_DEFS) {
  const [uid, , realName, major, skills, city] = d;
  const degree = d[9];
  if (!RESUME_USER_IDS.includes(uid)) continue;
  resumeId++;
  const u = userById.get(uid);

  const educations = [{
    school: '测试学院',
    major,
    degree,
    startDate: degree === '硕士' ? '2023-09' : '2022-09',
    endDate: degree === '硕士' ? '2026-06' : '2026-06',
    gpa: `${(3 + rnd() * 0.9).toFixed(2)}/4.0`,
  }];

  const projSkills = pickN(skills, Math.min(3, skills.length));
  const projects = [{
    name: `${major}综合实训 · ${projSkills[0]}实践项目`,
    role: pick(['负责人', '核心开发', '后端开发', '数据处理']),
    startDate: '2025-03',
    endDate: '2025-07',
    description: `使用 ${projSkills.join('、')} 搭建完整链路，独立完成核心模块设计与实现，`
      + `项目在课程答辩中获评优秀，代码已开源。`,
    skills: projSkills,
  }];
  if (rnd() < 0.6) {
    const more = pickN(skills, Math.min(2, skills.length));
    projects.push({
      name: `校园${pick(['信息服务', '数据分析', '实验管理', '竞赛训练'])}平台`,
      role: pick(['组员', '核心开发']),
      startDate: '2024-09',
      endDate: '2025-01',
      description: `参与需求梳理与 ${more.join('、')} 相关模块开发，负责其中约 40% 的编码量。`,
      skills: more,
    });
  }

  const internships = rnd() < 0.65 ? [{
    company: pick(COMPANIES),
    position: (INTENTION_BY_MAJOR[major] || '研发工程师') + '实习生',
    startDate: '2025-07',
    endDate: '2025-09',
    description: `参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。`,
  }] : [];

  const honors = pickN(HONOR_POOL, ri(1, 3));

  const selfIntro = `${major}专业${degree}在读，熟悉 ${skills.slice(0, 3).join('、')} 等技术，`
    + `有完整项目开发经验，具备良好的工程习惯与团队协作能力。`
    + `期望在${city}从事${INTENTION_BY_MAJOR[major] || '研发'}相关工作。`;

  resumeRows.push(
    `(${resumeId}, 1, ${uid}, '${u[5]}', '${u[6]}', '${esc(INTENTION_BY_MAJOR[major] || '研发工程师')}', `
    + `'${esc(selfIntro)}', '${esc(JSON.stringify(educations))}', '${esc(JSON.stringify(projects))}', `
    + `'${esc(JSON.stringify(internships))}', '${esc(JSON.stringify(honors))}')`
  );
}
L.push('-- ---------- 学生简历（只覆盖部分学生：未建简历的用于测 HR 端空态）----------');
L.push('INSERT INTO student_resume (id, tenant_id, user_id, contact_phone, contact_email, job_intention, self_intro, educations, projects, internships, honors) VALUES');
L.push(resumeRows.join(',\n') + ';');
L.push('');

L.push('-- ---------- 采集任务 ----------');
// MOCK 任务的 url_pattern 必须是 classpath 下 mock/ 目录里的真实文件名：
// CrawlerServiceImpl 会拼成 "mock/" + url_pattern 去加载。留 NULL 会拼出 "mock/null"
// （现在有默认值兜底，但种子不该依赖兜底）。
// status 一律给 0：这些任务并没有在跑，写 1 会让页面显示「运行中」而「停止」按钮报错。
L.push('INSERT INTO crawler_task (id, tenant_id, source_type, source_name, url_pattern, cron_expr, status, create_time) VALUES');
L.push([
  `(1, 1, 'MOCK', '模拟采集-mock-jobs.json', 'mock-jobs.json', '0 0 2 * * ?', 0, '2026-06-18 10:00:00')`,
    `(2, 1, 'OFFICIAL_PUBLIC', '人社部高校毕业生就业服务平台', 'http://job.mohrss.gov.cn/cjobs/lkysudi?pageNo=1&GJ=&job=计算机', NULL, 0, '2026-06-25 14:20:00')`,
  `(3, 2, 'MOCK', '模拟采集-示范大学', 'mock-jobs.json', NULL, 0, '2026-07-03 11:00:00')`,
].join(',\n') + ';');
L.push('');
L.push(`-- ---------- 采集日志（任务1 五次成功合计 ${mockJobCount} + 5 条待清洗/脏数据 = raw_job_data 总量）----------`);
const logRows = [];
let logId = 0;
BATCHES.forEach((b, i) => {
  logId++;
  // 最后一批多采 5 条（3 条待清洗 + 2 条脏数据）
  const extra = i === BATCHES.length - 1 ? 5 : 0;
  const end = new Date(b.time.getTime() + (b.count + extra) * 40 * 1000);
  logRows.push(`(${logId}, 1, 1, '${dt(b.time)}', '${dt(end)}', ${b.count + extra}, 'SUCCESS', NULL)`);
});
logId++;
logRows.push(`(${logId}, 2, 1, '2026-06-21 03:00:00', '2026-06-21 03:02:14', 0, 'FAILED', '连接目标站点超时（已重试 3 次）：www.zhipin.com 触发反爬验证')`);
logId++;
logRows.push(`(${logId}, 4, 1, '2026-07-09 04:00:00', NULL, 0, 'RUNNING', NULL)`);
L.push('INSERT INTO crawler_log (id, task_id, tenant_id, start_time, end_time, record_count, status, error_msg) VALUES');
L.push(logRows.join(',\n') + ';');
L.push('');
L.push('-- ---------- 原始职位数据（MOCK 职位的原始 JSON + 3 条待清洗 + 2 条脏数据）----------');
const rawRows = [];
let rawId = 0;
for (const j of jobs) {
  if (j.source !== 'MOCK') continue;
  rawId++;
  const raw = {
    title: j.title, company: j.company, city: j.city, industry: j.industry,
    salaryMin: j.salMin, salaryMax: j.salMax, education: j.edu, experience: j.exp,
    skills: j.skills, description: j.desc, publishDate: dOnly(j.publishDate),
  };
  rawRows.push(`(${rawId}, 'MOCK', '${j.sourceUrl}', '${esc(JSON.stringify(raw))}', '${dt(j.fetchTime)}', 'CLEANED')`);
}
// 3 条合法 RAW（尚未清洗，可用“存量清洗”功能验证入库）
const pendingSpecs = [
  { title: 'Python爬虫开发工程师', company: '数联云图大数据有限公司', city: '广州市', industry: '大数据', salaryMin: 9000, salaryMax: 14000, education: '本科', experience: '1-3年', skills: ['Python', 'Linux', 'MySQL'], description: '负责数据采集系统开发与维护。', publishDate: '2026-07-06' },
  { title: '云计算运维工程师', company: '华信云科技有限公司', city: '杭州市', industry: '互联网/IT', salaryMin: 10000, salaryMax: 16000, education: '本科', experience: '3-5年', skills: ['Linux', 'Docker', 'Kubernetes'], description: '负责云平台的部署、监控与故障处理。', publishDate: '2026-07-07' },
  { title: '数据标注专员', company: '博睿人工智能研究院有限公司', city: '西安市', industry: '人工智能', salaryMin: 5000, salaryMax: 7000, education: '专科', experience: '经验不限', skills: ['Excel', '数据标注'], description: '负责 AI 训练数据的标注与质检。', publishDate: '2026-07-08' },
];
for (const p of pendingSpecs) {
  rawId++;
  rawRows.push(`(${rawId}, 'MOCK', 'https://mock.occupation.dev/pending/${rawId}', '${esc(JSON.stringify(p))}', '2026-07-08 02:20:00', 'RAW')`);
}
// 2 条脏数据：非法 JSON、缺公司名（清洗时应被丢弃，验证容错逻辑）
rawId++;
rawRows.push(`(${rawId}, 'MOCK', 'https://mock.occupation.dev/dirty/${rawId}', '{title:非法JSON数据,,,', '2026-07-08 02:21:00', 'RAW')`);
rawId++;
rawRows.push(`(${rawId}, 'MOCK', 'https://mock.occupation.dev/dirty/${rawId}', '${esc(JSON.stringify({ title: '缺少公司名的职位', city: '北京', salaryMin: 8000, salaryMax: 12000 }))}', '2026-07-08 02:22:00', 'RAW')`);
L.push('INSERT INTO raw_job_data (id, source, source_url, raw_content, fetch_time, status) VALUES');
L.push(rawRows.join(',\n') + ';');
L.push('');
L.push('-- ---------- 清洗后职位 ----------');
const jobRows = jobs.map(j =>
  `(${j.id}, '${esc(j.title)}', '${esc(j.company)}', '${j.city}', '${esc(j.industry)}', ${j.salMin}, ${j.salMax}, '${j.edu}', '${j.exp}', '${esc(JSON.stringify(j.skills))}', '${esc(j.desc)}', '${dOnly(j.publishDate)}', '${j.source}', ${j.sourceUrl ? `'${j.sourceUrl}'` : 'NULL'}, ${j.publisherId ?? 'NULL'}, '${dt(j.createTime)}')`
);
L.push('INSERT INTO job_detail (id, title, company, city, industry, salary_min, salary_max, education, experience, skills, description, publish_date, source, source_url, publisher_id, create_time) VALUES');
L.push(jobRows.join(',\n') + ';');
L.push('');
L.push('-- ---------- 分析结果 ----------');
L.push('-- 职位维度（industry/city/skill/education/trend）：口径与 AnalysisJobServiceImpl 一致');
L.push('--   avg_salary = AVG((min+max)/2)。job_detail 是全平台共享表，所以两租户各一份相同的数据。');
L.push('-- 学生维度（apply_funnel/gap_*/student_*/contact_*）：口径与 EmploymentAnalysisContributor 一致');
L.push('--   这些是按租户隔离的，两租户的数字不同。预置它们是为了让「就业分析」页开箱有数据，');
L.push('--   否则新库打开是一堆空图表。管理员点「重算分析数据」会用后端口径覆盖，数值应当一致。');
const arRows = [];
let arId = 0;
const pushAr = (tid, dim, value, metric, val, period = '2026-07') => {
  arId++;
  arRows.push(`(${arId}, ${tid}, '${dim}', '${esc(value)}', '${metric}', ${val}, 'MONTH', '${period}', '${dt(CALC_TIME)}')`);
};

// ---- 与 Java 完全一致的取整：比值算到 4 位，落库统一 2 位（BigDecimal HALF_UP）----
const r4 = (part, total) => (total === 0 ? 0 : Math.round((part / total) * 1e4) / 1e4);
const r2 = (x) => Math.round((x + Number.EPSILON) * 100) / 100;
const medianOf = (sortedNums) => {
  if (!sortedNums.length) return 0;
  const n = sortedNums.length;
  return n % 2 ? sortedNums[(n - 1) / 2] : r2((sortedNums[n / 2 - 1] + sortedNums[n / 2]) / 2);
};

// 市场薪资中位数：全部职位的 (min+max)/2（job_detail 共享，两租户相同）
const marketSalaries = jobs.map(j => Math.trunc((j.salMin + j.salMax) / 2)).sort((a, b) => a - b);
const marketMedian = medianOf(marketSalaries);
// 岗位城市分布（共享）
const jobByCity = {};
for (const j of jobs) jobByCity[j.city] = (jobByCity[j.city] || 0) + 1;
const totalJobs = jobs.length;

const SALARY_LABELS = ['6000以下', '6000-8000', '8000-10000', '10000-15000', '15000以上'];
const SALARY_EDGES = [0, 6000, 8000, 10000, 15000, Infinity];
const bucketOf = (s) => SALARY_LABELS[SALARY_EDGES.findIndex((_, i) => s >= SALARY_EDGES[i] && s < SALARY_EDGES[i + 1])];

// 各租户的画像：租户1 是 STUDENT_DEFS，租户2 只有 T2_STUDENT
const profilesOf = (tid) => (tid === 1 ? STUDENT_DEFS : [T2_STUDENT]);

for (const tid of [1, 2]) {
  // ---- 职位维度（两租户相同）----
  for (const r of analysisRows) pushAr(tid, r.dim, r.value, r.metric, r.val, r.period);

  const profiles = profilesOf(tid);

  // ---- 投递漏斗：只统计本租户的 job_application（幽灵投递本就不存在于这张表）----
  const myApps = applications.filter(a => a.tenantId === tid);
  const counts = { SUBMITTED: 0, VIEWED: 0, INTERVIEW: 0, OFFER: 0, REJECTED: 0 };
  const respHours = [];
  for (const a of myApps) {
    counts[a.status]++;
    if (a.changedAt) {
      // 与 Java 的 Duration.toHours() 一致：向下取整
      respHours.push(Math.floor((a.changedAt - a.appliedAt) / 3600000));
    }
  }
  for (const s of Object.keys(counts)) {
    pushAr(tid, 'apply_funnel', s, 'application_count', counts[s]);
  }
  pushAr(tid, 'apply_funnel', 'TOTAL', 'application_count', myApps.length);
  respHours.sort((a, b) => a - b);
  pushAr(tid, 'apply_response', 'responded', 'application_count', respHours.length);
  pushAr(tid, 'apply_response', 'unresponded', 'application_count', counts.SUBMITTED);
  pushAr(tid, 'apply_response', 'median_hours', 'hours', r2(medianOf(respHours)));

  // ---- 学生意向分布 ----
  const stuByCity = {}, stuByInd = {}, stuBySalary = {};
  for (const l of SALARY_LABELS) stuBySalary[l] = 0;
  for (const p of profiles) {
    stuByCity[p[5]] = (stuByCity[p[5]] || 0) + 1;
    stuByInd[p[6]] = (stuByInd[p[6]] || 0) + 1;
    stuBySalary[bucketOf(p[7])]++;
  }
  for (const [k, v] of Object.entries(stuByCity)) pushAr(tid, 'student_city', k, 'student_count', v);
  for (const [k, v] of Object.entries(stuByInd)) pushAr(tid, 'student_industry', k, 'student_count', v);
  for (const l of SALARY_LABELS) pushAr(tid, 'student_salary', l, 'student_count', stuBySalary[l]);

  // ---- 供需错配（学生扎堆的前 10 个城市）----
  const totalStudents = profiles.length;
  const topCities = Object.entries(stuByCity).sort((a, b) => b[1] - a[1]).slice(0, 10);
  for (const [city, n] of topCities) {
    const sRatio = r4(n, totalStudents);
    const jRatio = r4(jobByCity[city] || 0, totalJobs);
    const gap = jRatio === 0 ? 999 : r2(sRatio / jRatio);
    pushAr(tid, 'gap_city', city, 'student_ratio', r2(sRatio * 100));
    pushAr(tid, 'gap_city', city, 'job_ratio', r2(jRatio * 100));
    pushAr(tid, 'gap_city', city, 'gap_ratio', gap);
  }
  const stuSalaries = profiles.map(p => Math.trunc((p[7] + p[8]) / 2)).sort((a, b) => a - b);
  const stuMedian = medianOf(stuSalaries);
  const deviation = marketMedian === 0 ? 0 : r2(Math.round(((stuMedian - marketMedian) / marketMedian) * 1e4) / 1e4 * 100);
  pushAr(tid, 'gap_salary', 'overall', 'student_median', r2(stuMedian));
  pushAr(tid, 'gap_salary', 'overall', 'market_median', r2(marketMedian));
  pushAr(tid, 'gap_salary', 'overall', 'deviation_percent', deviation);

  // ---- 自主求职流向 ----
  const contactByCity = {}, contactByInd = {};
  for (const b of behaviorsFinal.filter(b => b.action === 'CONTACT' && b.tenantId === tid)) {
    const j = jobById.get(b.jobId);
    contactByCity[j.city] = (contactByCity[j.city] || 0) + 1;
    contactByInd[j.industry] = (contactByInd[j.industry] || 0) + 1;
  }
  for (const [k, v] of Object.entries(contactByCity)) pushAr(tid, 'contact_city', k, 'contact_count', v);
  for (const [k, v] of Object.entries(contactByInd)) pushAr(tid, 'contact_industry', k, 'contact_count', v);
}
L.push('INSERT INTO analysis_result (id, tenant_id, dimension, dimension_value, metric_name, metric_value, period_type, period_value, calc_time) VALUES');
L.push(arRows.join(',\n') + ';');
L.push('');
L.push('-- ---------- 报告记录（一条历史失败样例；成功记录请在页面上现场生成。报告已无模板概念，由 大类+范围 直接生成）----------');
L.push('INSERT INTO report_record (id, tenant_id, name, category, params, file_url, file_type, status, error_msg, create_time) VALUES');
L.push(`(1, 1, '就业市场分析报告', 'MARKET', '{}', NULL, 'PDF', 'FAILED', '示例：一条历史失败记录（PDF 渲染失败）', '2026-06-28 14:12:00');`);
L.push('');

// ---------- 资讯（首页资讯板块）----------
// type: DATA_CAST=数据播报(平台数据自动生成,可点进图表) / ARTICLE=精选文章(有正文) / EXTERNAL=外部资讯(跳原文)
// cover_style: blue/green/purple/amber
const appStatusCounts = applications.reduce((m, a) => {
  m[a.status] = (m[a.status] || 0) + 1;
  return m;
}, {});
const platformJobCount = jobs.length - mockJobCount;
const industryCount = Object.keys(INDUSTRIES).length;
const offerCount = appStatusCounts.OFFER || 0;
const interviewCount = appStatusCounts.INTERVIEW || 0;

const NEWS = [
  // [id, category, type, title, summary, content, cover, source, sourceUrl, linkTarget, featured, publishTime]
  [1, null, 'DATA_CAST', `本平台在库岗位达 ${jobs.length} 个，数据分析已就绪`,
    `涵盖 ${mockJobCount} 个采集岗位与 ${platformJobCount} 个站内职位，覆盖 ${CITIES.length} 座主要城市、${industryCount} 大技术方向，看板与推荐均基于此。`,
    null, 'blue', '平台数据播报', null, '/admin/dashboard', 1, '2026-07-09 08:10:00'],
  [2, 'backend', 'DATA_CAST', 'Java 稳居技能热度榜首',
    '在全部岗位中，要求 Java 的职位数量最多，其后为 Python、MySQL。后端方向需求持续旺盛。',
    null, 'blue', '平台数据播报', null, '/admin/dashboard', 0, '2026-07-09 08:12:00'],
  [3, null, 'DATA_CAST', '上海、北京、深圳岗位最集中',
    '按城市分布，上海岗位数领先，北京、深圳紧随其后；杭州、成都为新一线热门去向。',
    null, 'green', '平台数据播报', null, '/admin/dashboard', 0, '2026-07-09 08:14:00'],
  [4, 'bigdata', 'DATA_CAST', '大数据方向平均薪资领先',
    '按行业统计，大数据 / 人工智能方向平均薪资高于全站均值，Spark、Flink 等技能溢价明显。',
    null, 'purple', '平台数据播报', null, '/admin/employment', 0, '2026-07-09 08:16:00'],
  [5, null, 'DATA_CAST', `投递转化：${applications.length} 份投递中已产生 ${offerCount} 个 OFFER`,
    `站内投递共 ${applications.length} 份，其中面试阶段 ${interviewCount} 份、录用 ${offerCount} 份；及时完善画像与简历有助于提升转化。`,
    null, 'amber', '平台数据播报', null, '/admin/employment', 0, '2026-07-09 08:18:00'],
  [6, 'backend', 'ARTICLE', '2026 后端开发就业观察：微服务与云原生成标配',
    '从平台岗位要求看，Spring Boot、微服务、容器化几乎成为后端岗位的默认门槛。',
    '<p>综合本平台采集到的后端岗位数据，Spring Boot、Spring Cloud、Redis、消息队列（Kafka/RocketMQ）出现频率显著上升。</p><p>建议在校生优先夯实 Java 基础与数据库，再向微服务、容器化（Docker/K8s）延伸，配合一到两个完整项目经历，将明显提升竞争力。</p>',
    'blue', '就业指导中心', null, null, 1, '2026-07-06 10:00:00'],
  [7, 'frontend', 'ARTICLE', '前端招聘趋势：工程化与框架深度并重',
    '企业更看重 Vue/React 的工程化实践与组件设计能力，而非仅会写页面。',
    '<p>平台前端岗位中，Vue、TypeScript、构建工具（Vite/Webpack）、组件化设计是高频关键词。</p><p>建议同学在掌握一个主流框架的基础上，理解其响应式原理与工程化配置，并积累可展示的项目。</p>',
    'green', '就业指导中心', null, null, 0, '2026-07-05 14:30:00'],
  [8, null, 'EXTERNAL', '示例：外部就业资讯（接入开源中国 RSS 后在此展示）',
    '外部资讯仅展示标题与摘要，点击「阅读原文」跳转来源站点；封面用色块占位。',
    null, 'purple', '开源中国', 'https://www.oschina.net/news', null, 0, '2026-07-08 09:00:00'],
];
L.push('-- ---------- 资讯（DATA_CAST 数据播报 / ARTICLE 精选文章 / EXTERNAL 外部资讯占位）----------');
L.push('INSERT INTO news (id, tenant_id, category, type, title, summary, content, cover_style, source, source_url, link_target, view_count, featured, status, publish_time) VALUES');
L.push(NEWS.map(n => {
  const q = (v) => v == null ? 'NULL' : `'${esc(v)}'`;
  return `(${n[0]}, 1, ${q(n[1])}, '${n[2]}', ${q(n[3])}, ${q(n[4])}, ${q(n[5])}, '${n[6]}', ${q(n[7])}, ${q(n[8])}, ${q(n[9])}, ${ri(20, 320)}, ${n[10]}, 1, '${n[11]}')`;
}).join(',\n') + ';');
L.push('');
L.push('-- ---------- 资讯扩充：200 条（5 分类 × 3 类型，全部附带 picsum.photos 封面图）----------');
L.push('INSERT INTO news (id, tenant_id, category, type, title, summary, content, cover_style, cover_image, source, source_url, link_target, view_count, featured, status, publish_time) VALUES');
L.push(readBulkNewsRows() + ';');
L.push('');

L.push('-- ---------- 推送记录 ----------');
const pushRows = pushes.map((p, i) =>
  `(${i + 1}, ${p.tenantId}, ${p.userId}, '${p.type}', '${esc(p.title)}', '${esc(p.content)}', ${p.isRead}, '${dt(p.time)}')`
);
L.push('INSERT INTO push_record (id, tenant_id, user_id, type, title, content, is_read, create_time) VALUES');
L.push(pushRows.join(',\n') + ';');
L.push('');
L.push('-- ---------- 学生行为（VIEW→FAVORITE→APPLY 时间有序，职位均真实存在）----------');
const behRows = behaviorsFinal.map((b, i) =>
  `(${i + 1}, ${b.tenantId}, ${b.userId}, ${b.jobId}, '${b.action}', '${dt(b.time)}')`
);
L.push('INSERT INTO student_behavior (id, tenant_id, user_id, job_id, action, create_time) VALUES');
L.push(behRows.join(',\n') + ';');
L.push('');

L.push('-- ---------- 投递记录（与 student_behavior 的 APPLY 一一对应，状态铺开五种）----------');
L.push('INSERT INTO job_application (id, tenant_id, user_id, job_id, publisher_id, status, hr_note, applied_at, status_changed_at, create_time) VALUES');
L.push(applications.map((a, i) =>
  `(${i + 1}, ${a.tenantId}, ${a.userId}, ${a.jobId}, ${a.publisherId}, '${a.status}', ` +
  `${a.note ? `'${esc(a.note)}'` : 'NULL'}, '${dt(a.appliedAt)}', ${a.changedAt ? `'${dt(a.changedAt)}'` : 'NULL'}, '${dt(a.appliedAt)}')`
).join(',\n') + ';');
L.push('');

L.push('-- ---------- API 客户端 ----------');
L.push('-- api_secret 以 BCrypt 存储（与 sys_user.password_hash 同一套编码器）。');
L.push('-- 调用 /api/open/auth/token 时传明文 secret，服务端用 passwordEncoder.matches 比对：');
L.push('--   occ_test_2026       → demo_secret_key_for_dev');
L.push('--   occ_portal_2026     → portal_secret_key_for_dev');
L.push('--   occ_screen_2025     → screen_secret_key_deprecated（客户端已停用）');
L.push('--   occ_demo_univ_2026  → demo_univ_secret_key（租户 2）');
L.push('INSERT INTO api_client (id, tenant_id, client_name, api_key, api_secret, scopes, status, create_time) VALUES');
L.push([
  `(1, 1, '测试客户端', 'occ_test_2026', '${API_SECRET_HASHES.demo}', 'jobs:read,reports:read,skills:read', 1, '2026-06-18 10:00:00')`,
  `(2, 1, '就业信息门户', 'occ_portal_2026', '${API_SECRET_HASHES.portal}', 'jobs:read,skills:read', 1, '2026-06-30 11:20:00')`,
  `(3, 1, '旧数据大屏（已停用）', 'occ_screen_2025', '${API_SECRET_HASHES.screen}', 'jobs:read', 0, '2026-06-18 10:10:00')`,
  `(4, 2, '示范大学门户', 'occ_demo_univ_2026', '${API_SECRET_HASHES.demoUniv}', 'jobs:read,reports:read', 1, '2026-07-03 11:30:00')`,
].join(',\n') + ';');
L.push('');
L.push('-- ---------- 系统告警（CRAWLER_FAILURE 与 crawler_log 失败记录时间对应）----------');
const tenant1AnalysisCount = analysisRows.length;
L.push('INSERT INTO sys_alert (id, tenant_id, type, level, content, is_read, create_time) VALUES');
L.push([
  `(1, 1, 'SYSTEM', 'INFO', '系统初始化完成，欢迎使用职业能力大数据服务平台', 1, '2026-06-18 10:00:00')`,
  `(2, 1, 'CRAWLER_FAILURE', 'ERROR', 'Boss直聘采集任务失败：连接目标站点超时（已重试 3 次），任务已自动停用', 1, '2026-06-21 03:02:15')`,
  `(3, 1, 'KAFKA_LAG', 'WARN', 'Kafka 消费组 raw-job-data-group 消息积压 1240 条，请关注清洗服务消费速率', 1, '2026-07-05 02:40:00')`,
  `(4, 1, 'DB_POOL', 'WARN', '数据库连接池使用率达 85%（17/20），高峰期请留意慢查询', 0, '2026-07-08 15:22:00')`,
  `(5, 1, 'SYSTEM', 'INFO', '统计分析任务完成：本次重算写入 ${tenant1AnalysisCount} 条结果（industry/city/education/skill/trend 五个维度）', 0, '${dt(CALC_TIME)}')`,
  `(6, 2, 'SYSTEM', 'INFO', '系统初始化完成，欢迎使用职业能力大数据服务平台', 0, '2026-07-03 11:00:00')`,
].join(',\n') + ';');
L.push('');

// ---------- 拼接进 init.sql ----------
const orig = fs.readFileSync(INIT_SQL, 'utf8');
const lines = orig.split(/\r?\n/);
const seedIdx = lines.findIndex(l => l.includes('初始化种子数据'));
if (seedIdx < 0) throw new Error('未找到种子数据段落');
// 回退到该段落的分隔行（前一行是 ====）
const cutAt = lines[seedIdx - 1].startsWith('-- ====') ? seedIdx - 1 : seedIdx;
const head = lines.slice(0, cutAt).join('\n');
fs.writeFileSync(INIT_SQL, head + '\n' + L.join('\n') + '\n', 'utf8');

// ---------- 汇总 ----------
console.log('=== 生成完成 ===');
console.log('职位总数:', jobs.length, '（MOCK:', mockJobCount, '+ HR_PUBLISH:', jobs.length - mockJobCount, '）');
console.log('raw_job_data:', rawId, '（CLEANED:', mockJobCount, '+ RAW 待清洗: 3 + 脏数据: 2）');
console.log('采集批次 record_count:', BATCHES.map(b => b.count), '+ 最后批次额外 5');
console.log('用户:', users.length, '（HR', users.filter(u => u[3] === 'HR').length, '）',
  '画像:', profId, '简历:', resumeId, '推送:', pushes.length);
const byAction = {};
for (const b of behaviorsFinal) byAction[b.action] = (byAction[b.action] || 0) + 1;
console.log('行为:', behaviorsFinal.length, byAction);
const byStatus = {};
for (const a of applications) byStatus[a.status] = (byStatus[a.status] || 0) + 1;
console.log('投递记录:', applications.length, byStatus);
const perHr = {};
for (const j of jobs.filter(j => j.source === 'HR_PUBLISH')) perHr[j.publisherId] = (perHr[j.publisherId] || 0) + 1;
console.log('站内职位按 HR:', perHr);
console.log('分析结果: 每租户', analysisRows.length, '条 × 2 租户 =', arRows.length);
const byMonth = {};
for (const j of jobs) { const k = pad(j.publishDate.getMonth() + 1); byMonth[k] = (byMonth[k] || 0) + 1; }
console.log('职位按月分布:', byMonth);
