import { createRouter, createWebHistory } from 'vue-router'

const HomeIndex = () => import('@/views/Home/HomeIndex.vue')

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
      { path: '', name: 'AdminHome', component: HomeIndex, meta: { layout: 'full' } },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'employment', name: 'Employment', component: () => import('@/views/admin/Employment.vue') },
      { path: 'crawler', name: 'CrawlerTask', component: () => import('@/views/admin/CrawlerTask.vue') },
      { path: 'report-template', name: 'ReportTemplate', component: () => import('@/views/admin/ReportTemplate.vue') },
      { path: 'report-list', name: 'ReportList', component: () => import('@/views/admin/ReportList.vue') },
      { path: 'user', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue') }
    ]
  },

  {
    path: '/student',
    component: () => import('@/components/MainLayout.vue'),
    meta: { role: 'STUDENT' },
    children: [
      { path: '', name: 'StudentHome', component: HomeIndex, meta: { layout: 'full' } },
      { path: 'jobs', name: 'StudentJobs', component: () => import('@/views/student/StudentHome.vue') },
      { path: 'job/:id', name: 'JobDetail', component: () => import('@/views/student/JobDetail.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/student/Profile.vue') },
      { path: 'resume', name: 'Resume', component: () => import('@/views/student/Resume.vue') },
      { path: 'applications', name: 'MyApplications', component: () => import('@/views/student/Applications.vue') },
      { path: 'advisor', name: 'Advisor', component: () => import('@/views/student/Advisor.vue') },
      { path: 'favorites', name: 'Favorites', component: () => import('@/views/student/Favorites.vue') },
      { path: 'reports', name: 'StudentReports', component: () => import('@/views/student/Reports.vue') }
    ]
  },

  {
    path: '/teacher',
    component: () => import('@/components/MainLayout.vue'),
    meta: { role: 'TEACHER' },
    children: [
      { path: '', name: 'TeacherHome', component: HomeIndex, meta: { layout: 'full' } },
      { path: 'students', name: 'Students', component: () => import('@/views/teacher/Students.vue') },
      { path: 'suggestions', name: 'Suggestions', component: () => import('@/views/teacher/Suggestions.vue') }
    ]
  },

  {
    path: '/hr',
    component: () => import('@/components/MainLayout.vue'),
    meta: { role: 'HR' },
    children: [
      { path: '', name: 'HrHome', component: HomeIndex, meta: { layout: 'full' } },
      { path: 'jobs', name: 'JobManage', component: () => import('@/views/hr/JobManage.vue') },
      { path: 'applications', name: 'HrApplications', component: () => import('@/views/hr/Applications.vue') },
      { path: 'talents', name: 'Talents', component: () => import('@/views/hr/Talents.vue') }
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
