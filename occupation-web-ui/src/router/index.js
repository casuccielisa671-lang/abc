import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // ========== 通用 ==========
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue')
  },

  // ========== 管理后台 ==========
  {
    path: '/admin',
    component: () => import('@/components/AppLayout.vue'),
    meta: { role: 'ADMIN' },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'crawler', name: 'CrawlerTask', component: () => import('@/views/admin/CrawlerTask.vue') },
      { path: 'report-template', name: 'ReportTemplate', component: () => import('@/views/admin/ReportTemplate.vue') },
      { path: 'report-list', name: 'ReportList', component: () => import('@/views/admin/ReportList.vue') },
      { path: 'user', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue') }
    ]
  },

  // ========== 学生端 ==========
  {
    path: '/student',
    component: () => import('@/components/AppLayout.vue'),
    meta: { role: 'STUDENT' },
    children: [
      { path: '', name: 'StudentHome', component: () => import('@/views/student/StudentHome.vue') },
      { path: 'job/:id', name: 'JobDetail', component: () => import('@/views/student/JobDetail.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/student/Profile.vue') },
      { path: 'favorites', name: 'Favorites', component: () => import('@/views/student/Favorites.vue') },
      { path: 'reports', name: 'StudentReports', component: () => import('@/views/student/Reports.vue') }
    ]
  },

  // ========== 教师端 ==========
  {
    path: '/teacher',
    component: () => import('@/components/AppLayout.vue'),
    meta: { role: 'TEACHER' },
    children: [
      { path: '', name: 'TeacherHome', component: () => import('@/views/teacher/TeacherHome.vue') },
      { path: 'students', name: 'Students', component: () => import('@/views/teacher/Students.vue') },
      { path: 'suggestions', name: 'Suggestions', component: () => import('@/views/teacher/Suggestions.vue') }
    ]
  },

  // ========== 企业 HR 端 ==========
  {
    path: '/hr',
    component: () => import('@/components/AppLayout.vue'),
    meta: { role: 'HR' },
    children: [
      { path: '', name: 'HrHome', component: () => import('@/views/hr/HrHome.vue') },
      { path: 'jobs', name: 'JobManage', component: () => import('@/views/hr/JobManage.vue') },
      { path: 'talents', name: 'Talents', component: () => import('@/views/hr/Talents.vue') }
    ]
  },

  // ========== 默认重定向 ==========
  { path: '/', redirect: '/login' },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// TODO: 路由守卫 — 校验 JWT Token 有效期 + 角色权限
export default router
