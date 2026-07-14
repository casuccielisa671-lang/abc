import { createRouter, createWebHistory } from 'vue-router'


const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { guest: true }
  },

  {
    path: '/admin',
    component: () => import('@/components/MainLayout.vue'),
    meta: { role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/Analysis.vue') },
      { path: 'employment', name: 'Employment', component: () => import('@/views/admin/Analysis.vue') },
      { path: 'crawler', name: 'CrawlerTask', component: () => import('@/views/admin/CrawlerTask.vue') },
      { path: 'report-list', name: 'ReportList', component: () => import('@/views/admin/ReportList.vue') },
      { path: 'user', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'class', name: 'ClassManage', component: () => import('@/views/admin/ClassManage.vue') },
      { path: 'news', name: 'AdminNews', component: () => import('@/views/common/NewsPage.vue') },
      { path: 'news-manage', name: 'NewsManage', component: () => import('@/views/admin/NewsManage.vue') },
      { path: 'messages', name: 'AdminMessages', component: () => import('@/views/common/MessagePage.vue') },
      { path: 'map', name: 'AdminMap', component: () => import('@/views/common/MapExplore.vue') },
      { path: 'tools', name: 'AdminToolbox', component: () => import('@/views/admin/Toolbox.vue') },
      { path: 'tools/export-center', name: 'ExportCenter', component: () => import('@/views/admin/tools/ExportCenter.vue') },
      { path: 'tools/tenant-health', name: 'TenantHealth', component: () => import('@/views/admin/tools/TenantHealth.vue') }
    ]
  },

  {
    path: '/student',
    component: () => import('@/components/MainLayout.vue'),
    meta: { role: 'STUDENT' },
    children: [
      { path: '', name: 'StudentHome', component: () => import('@/views/student/StudentDashboard.vue') },
      { path: 'jobs', name: 'StudentJobs', component: () => import('@/views/student/StudentHome.vue') },
      { path: 'job/:id', name: 'JobDetail', component: () => import('@/views/student/JobDetail.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/student/MyData.vue') },
      { path: 'resume', name: 'Resume', component: () => import('@/views/student/MyData.vue') },
      { path: 'applications', name: 'MyApplications', component: () => import('@/views/student/StudentHome.vue') },
      { path: 'advisor', name: 'Advisor', component: () => import('@/views/student/Advisor.vue') },
      { path: 'favorites', name: 'Favorites', component: () => import('@/views/student/StudentHome.vue') },
      { path: 'reports', name: 'StudentReports', component: () => import('@/views/student/Reports.vue') },
      { path: 'news', name: 'StudentNews', component: () => import('@/views/common/NewsPage.vue') },
      { path: 'messages', name: 'StudentMessages', component: () => import('@/views/common/MessagePage.vue') },
      { path: 'map', name: 'StudentMap', component: () => import('@/views/common/MapExplore.vue') },
      { path: 'tools', name: 'StudentToolbox', component: () => import('@/views/student/Toolbox.vue') },
      { path: 'tools/compare-jobs', name: 'CompareJobs', component: () => import('@/views/student/tools/CompareJobs.vue') },
      { path: 'tools/skill-roi', name: 'SkillRoi', component: () => import('@/views/student/tools/SkillRoi.vue') },
      { path: 'tools/salary-calc', name: 'SalaryCalc', component: () => import('@/views/student/tools/SalaryCalc.vue') },
      { path: 'tools/job-checklist', name: 'JobChecklist', component: () => import('@/views/student/tools/JobChecklist.vue') }
    ]
  },

  {
    path: '/teacher',
    component: () => import('@/components/MainLayout.vue'),
    meta: { role: 'TEACHER' },
    children: [
      { path: '', name: 'TeacherHome', component: () => import('@/views/teacher/TeacherDashboard.vue') },
      { path: 'students', name: 'Students', component: () => import('@/views/teacher/Students.vue') },
      { path: 'suggestions', name: 'Suggestions', component: () => import('@/views/teacher/Suggestions.vue') },
      { path: 'news', name: 'TeacherNews', component: () => import('@/views/common/NewsPage.vue') },
      { path: 'messages', name: 'TeacherMessages', component: () => import('@/views/common/MessagePage.vue') },
      { path: 'map', name: 'TeacherMap', component: () => import('@/views/common/MapExplore.vue') },
      { path: 'tools', name: 'TeacherToolbox', component: () => import('@/views/teacher/Toolbox.vue') },
      { path: 'tools/compare-classes', name: 'CompareClasses', component: () => import('@/views/teacher/tools/CompareClasses.vue') },
      { path: 'tools/student-alerts', name: 'StudentAlerts', component: () => import('@/views/teacher/tools/StudentAlerts.vue') },
      { path: 'tools/course-match', name: 'CourseMatch', component: () => import('@/views/teacher/tools/CourseMatch.vue') }
    ]
  },

  {
    path: '/hr',
    component: () => import('@/components/MainLayout.vue'),
    meta: { role: 'HR' },
    children: [
      { path: '', name: 'HrHome', component: () => import('@/views/hr/HrDashboard.vue') },
      { path: 'jobs', name: 'JobManage', component: () => import('@/views/hr/JobManage.vue') },
      { path: 'applications', name: 'HrApplications', component: () => import('@/views/hr/Applications.vue') },
      { path: 'talents', name: 'Talents', component: () => import('@/views/hr/Talents.vue') },
      { path: 'news', name: 'HrNews', component: () => import('@/views/common/NewsPage.vue') },
      { path: 'messages', name: 'HrMessages', component: () => import('@/views/common/MessagePage.vue') },
      { path: 'map', name: 'HrMap', component: () => import('@/views/common/MapExplore.vue') },
      { path: 'tools', name: 'HrToolbox', component: () => import('@/views/hr/Toolbox.vue') },
      { path: 'tools/compare-talents', name: 'CompareTalents', component: () => import('@/views/hr/tools/CompareTalents.vue') },
      { path: 'tools/salary-benchmark', name: 'SalaryBenchmark', component: () => import('@/views/hr/tools/SalaryBenchmark.vue') }
    ]
  },

  { path: '/', redirect: '/login' },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.guest) {
    if (token) {
      const role = localStorage.getItem('role')
      const ROLE_HOME = { ADMIN: '/admin', STUDENT: '/student', TEACHER: '/teacher', HR: '/hr' }
      next(ROLE_HOME[role] || '/admin')
    } else {
      next()
    }
    return
  }

  if (!token) {
    next('/login')
    return
  }

  const requiredRole = to.meta.role
  if (requiredRole) {
    const userRole = localStorage.getItem('role')
    if (userRole !== requiredRole && userRole !== 'ADMIN') {
      const ROLE_HOME = { ADMIN: '/admin', STUDENT: '/student', TEACHER: '/teacher', HR: '/hr' }
      next(ROLE_HOME[userRole] || '/login')
      return
    }
    // 就业分析仅管理员可访问
    if (to.path === '/admin/employment' && userRole !== 'ADMIN') {
      next('/admin')
      return
    }
  }

  next()
})

export default router
