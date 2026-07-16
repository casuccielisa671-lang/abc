import {
  DataAnalysis,
  Setting,
  Document,
  User,
  House,
  Reading,
  TrendCharts,
  Management,
  Tickets,
  ChatDotRound,
  Promotion,
  Notebook
} from '@element-plus/icons-vue'

const MENUS = {
  ADMIN: [
    { index: '/admin', title: '首页', icon: House },
    { index: '/admin/dashboard', title: '数据看板', icon: DataAnalysis },
    { index: '/admin/employment', title: '就业分析', icon: TrendCharts },
    { index: '/admin/crawler', title: '采集管理', icon: Setting },
    { index: '/admin/report-list', title: '报告中心', icon: Document },
    { index: '/admin/user', title: '用户管理', icon: User },
    { index: '/admin/class', title: '班级管理', icon: Reading },
    { index: '/admin/news-manage', title: '资讯管理', icon: Notebook },
    {
      index: '/admin/tools',
      title: '工具箱',
      icon: Setting,
      children: [
        { index: '/admin/tools/export-center', title: '数据导出' },
        { index: '/admin/tools/tenant-health', title: '租户健康度' }
      ]
    }
  ],
  STUDENT: [
    { index: '/student', title: '首页', icon: House },
    { index: '/student/jobs', title: '职位推荐', icon: Promotion },
    { index: '/student/profile', title: '个人画像', icon: User },
    { index: '/student/reports', title: '我的报告', icon: Document },
    { index: '/student/advisor', title: '职业顾问', icon: ChatDotRound },
    { index: '/student/news', title: '资讯', icon: Notebook },
    {
      index: '/student/tools',
      title: '工具箱',
      icon: Setting,
      children: [
        { index: '/student/tools/compare-jobs', title: '多岗位对比' },
        { index: '/student/tools/skill-roi', title: '技能ROI' },
        { index: '/student/tools/salary-calc', title: '薪资计算器' },
        { index: '/student/tools/job-checklist', title: '求职清单' }
      ]
    }
  ],
  TEACHER: [
    { index: '/teacher', title: '首页', icon: House },
    { index: '/teacher/students', title: '学生管理', icon: Reading },
    { index: '/teacher/suggestions', title: '教学建议', icon: TrendCharts },
    { index: '/teacher/news', title: '资讯', icon: Notebook },
    {
      index: '/teacher/tools',
      title: '工具箱',
      icon: Setting,
      children: [
        { index: '/teacher/tools/compare-classes', title: '班级对比' },
        { index: '/teacher/tools/student-alerts', title: '学生预警' },
        { index: '/teacher/tools/course-match', title: '课程匹配' }
      ]
    }
  ],
  HR: [
    { index: '/hr', title: '首页', icon: House },
    { index: '/hr/jobs', title: '职位管理', icon: Management },
    { index: '/hr/applications', title: '收到的投递', icon: Tickets },
    { index: '/hr/talents', title: '人才浏览', icon: User },
    { index: '/hr/news', title: '资讯', icon: Notebook },
    {
      index: '/hr/tools',
      title: '工具箱',
      icon: Setting,
      children: [
        { index: '/hr/tools/compare-talents', title: '人才对比' },
        { index: '/hr/tools/salary-benchmark', title: '薪资竞争力' }
      ]
    }
  ]
}

export function getMenusByRole(role) {
  return MENUS[role] || []
}
