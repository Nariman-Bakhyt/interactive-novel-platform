<script setup lang="ts">
import { onMounted, ref, onUnmounted } from 'vue';
import { getNewNovels } from "@/api/novelService.ts";
import type { NovelResponseDto } from "@/types/novel.ts";
import NovelCard from "@/components/NovelCard.vue";
import router from "@/router"; // Импортируем нашу карточку!

const novels = ref<NovelResponseDto[]>([]);
const isLoading = ref(true);
const isMobile = ref(false);

const fetchNovels = async () => {
  try {
    isLoading.value = true;
    const data = await getNewNovels();
    novels.value = data.content;
  } catch (error) {
    console.error('Ошибка при загрузке новелл:', error);
  } finally {
    isLoading.value = false;
  }
};

const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768;
};

const goToNovelDetail = (id: number) => {
  router.push({ name: 'NovelDetail', params: { id: id.toString() } });
};

onMounted(() => {
  fetchNovels();
  checkMobile();
  window.addEventListener('resize', checkMobile);
});

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile);
});
</script>

<template>
  <div class="page-wrapper">
    <section class="hero-section">
      <div class="overlay"></div>
      <div class="content">
        <h1 class="title">Read<span class="accent">Hub</span></h1>
        <p class="subtitle">Погружайся в миры, созданные сообществом.</p>
        <router-link to="/novels" class="btn primary">Начать читать</router-link>
      </div>
    </section>

    <section class="novels-container">
      <div class="section-header">
        <h2>Свежие новинки</h2>
        <router-link to="/novels" class="view-all">Смотреть все →</router-link>
      </div>

      <div v-if="isLoading" class="loader">Загрузка...</div>

      <div v-else class="novels-grid">
        <NovelCard
          v-for="novel in novels"
          :key="novel.id"
          :novel="novel"
          :is-mobile="isMobile"
          @click="goToNovelDetail"
        />
      </div>
    </section>
  </div>
</template>
<style scoped>
.page-wrapper { background-color: var(--bg-main); min-height: 100vh; color: var(--text-header); }
.novels-container { max-width: 1200px; margin: -50px auto 0; padding: 0 20px 100px; position: relative; z-index: 20; }
.novels-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 2rem; align-items: stretch; }

/* --- HERO СЕКЦИЯ (ГЛАВНЫЙ ЭКРАН) --- */
.hero-section { position: relative; height: 80vh; display: flex; justify-content: center; align-items: center; background: url('https://images.unsplash.com/photo-1519681393784-d120267933ba?q=80&w=2070&auto=format&fit=crop') center/cover; }
.overlay { position: absolute; inset: 0; background: linear-gradient(to bottom, rgba(15, 23, 42, 0.4), var(--bg-main)); }
.content { position: relative; z-index: 10; text-align: center; }
.accent { color: #6366f1; }
.title { font-size: 4rem; margin-bottom: 1rem; }
.subtitle { font-size: 1.2rem; margin-bottom: 2rem; color: #ddd; }
.btn.primary {
  padding: 1rem 2rem;
  background: #6366f1;
  border: none;
  color: white;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  transition: transform 0.2s;
  /* Добавлено для корректного отображения ссылки как кнопки */
  text-decoration: none;
  display: inline-block;
}
.btn.primary:hover { transform: scale(1.05); }

/* --- UI ЭЛЕМЕНТЫ --- */
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
.view-all { color: var(--btn-plus); text-decoration: none; font-weight: 600; }
.view-all:hover { text-decoration: underline; }
.loader { text-align: center; padding: 50px; color: var(--text-muted); }
</style>
