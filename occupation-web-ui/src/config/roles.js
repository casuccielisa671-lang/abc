export const LOGIN_ROLES = [
  { value: 'STUDENT', label: '学生', placeholder: '学号 / 用户名' },
  { value: 'TEACHER', label: '教师', placeholder: '工号 / 用户名' },
  { value: 'HR', label: '企业 HR', placeholder: '用户名' },
  { value: 'ADMIN', label: '管理员', placeholder: '管理员用户名' }
]

export const ROLE_HOME = {
  ADMIN: '/admin',
  STUDENT: '/student',
  TEACHER: '/teacher',
  HR: '/hr'
}

export const ROLE_LABEL = {
  ADMIN: '管理员',
  STUDENT: '学生',
  TEACHER: '教师',
  HR: '企业 HR'
}

export function getRoleHome(role) {
  return ROLE_HOME[role] || '/admin'
}

export function getRoleLabel(role) {
  return ROLE_LABEL[role] || role || ''
}
