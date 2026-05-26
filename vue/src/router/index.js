import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { requiresAuth: true }
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/logs',
    name: 'logs',
    component: () => import('../views/LogView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/roles',
    name: 'roles',
    component: () => import('../views/RoleView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else if (to.meta.requiresAuth) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const menus = Array.isArray(userInfo.menus) ? userInfo.menus : []
    const allowedPaths = menus.map(menu => menu.path)
    if (allowedPaths.length > 0 && !allowedPaths.includes(to.path)) {
      next(allowedPaths[0] || '/')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
