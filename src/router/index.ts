import { createRouter, createWebHistory } from 'vue-router';
import {useAuthStore} from "@/api/auth.ts";
import HomeView from "@/views/HomeView.vue";
import MainLayout from "@/views/MainLayout.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, from, savedPosition) {
    if (to.hash) {
      return {
        el: to.hash,
        behavior: 'smooth',
      }
    }
    return savedPosition || { top: 0 }
  },
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView ,
        },
        {
          path:'profile',
          name:'profile',
          component: ()=>import('@/views/ProfileView.vue'),
          meta:{ requiresAuth: true },
        },
        {
          path: '/my-novels',
          name: 'MyNovels',
          component: () => import('@/views/novels/MyNovels.vue'),
          meta: { requiresAuth: true } // Помечаем, что нужен вход
        },
        {
          path: '/novels/create',
          name: 'CreateNovel',
          component: () => import('@/views/novels/NovelEditor.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/novels/:id/edit',
          name: 'EditNovel',
          component: () => import('@/views/novels/NovelEditor.vue'),
          meta: { requiresAuth: true },
          props: true // Чтобы ID из ссылки попал в props компонента
        },
        {
          path: '/novel/:id',
          name: 'NovelDetail',
          component: () => import('@/views/novels/NovelDetailView.vue'),
          props: true
        },
        {
          path: '/novels/:novelId/chapters/create',
          name: 'createChapter',
          component: () => import('@/views/novels/chapter/ChapterEditor.vue'),
          meta: { requiresAuth: true },
          props: true //novelId придет в компонент как пропс
        },
        {
          path: '/novels/:novelId/chapters/:chapterId/edit',
          name: 'editChapter',
          component: () => import('@/views/novels/chapter/ChapterEditor.vue'),
          meta: { requiresAuth: true },
          props: true
        },
        {
          path: '/novels/:novelId/chapter/:chapterId',
          name: 'ChapterDetaill',
          component: () => import('@/views/novels/chapter/ChapterDetail.vue'),
          props: true
        },
        {
          path: '/novels',
          name: 'CatalogView',
          component: () => import('@/views/novels/CatalogView.vue'),
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
