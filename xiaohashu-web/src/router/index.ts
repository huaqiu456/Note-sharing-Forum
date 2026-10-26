import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

const TOKEN_KEY = "xiaohashu_token"

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', redirect: '/home' },
        { path: 'home', name: 'home', component: () => import('../views/HomeView.vue') },
        { path: 'publish', name: 'publish', component: () => import('../views/NotePublishView.vue') },
        { path: 'note/:id', name: 'note-detail', component: () => import('../views/NoteDetailView.vue') },
        { path: 'search', name: 'search', component: () => import('../views/SearchView.vue') },
        { path: 'profile', name: 'profile', component: () => import('../views/ProfileView.vue') }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!to.meta.public && !token) return { name: 'login' }
  if (to.name === 'login' && token) return { name: 'home' }
  return true
})

export default router
