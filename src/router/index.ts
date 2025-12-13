import { createRouter, createWebHistory } from 'vue-router';
import {useAuthStore} from "@/stores/auth.ts";
import HomeView from "@/views/HomeView.vue";
import MainLayout from "@/views/MainLayout.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: MainLayout, // Используем Layout для всех внутренних страниц
      children: [
        {
          path: '/',
          name: 'home',
          component: HomeView ,
        },
        {
          path:'profile',
          name:'profile',
          component: ()=>import('@/views/ProfileView.vue'),
          meta:{ requiresAuth: true },
        }
      ]
    }
  ]
})

router.beforeEach((to, from) => {
  const authStore = useAuthStore();

  if(to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      path:'/',
      query:{redirect:to.fullPath},
    };
  }
  return true;
})

export default router
