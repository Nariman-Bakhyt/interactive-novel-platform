<script setup lang="ts">
import {onMounted, onUnmounted, ref} from 'vue';
import {getNewNovels} from "@/api/novelService.ts";
import type {NovelResponseDto} from "@/types/novel.ts";
import NovelCard from "@/components/NovelCard.vue";
import router from "@/router"; 

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
      <div class="content">
        <h1 class="title">Wén<span class="accent">Lib</span></h1>
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
.page-wrapper {
  background-color: var(--bg-main);
  min-height: 100vh;
  color: var(--text-header);
}

/* ── Hero ── */
.hero-section {
  position: relative;
  height: 65vh;
  min-height: 420px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  /* layered gradient background */
  background:
    radial-gradient(ellipse 70% 70% at 50% -10%, rgba(99,102,241,0.2) 0%, transparent 65%),
    radial-gradient(ellipse 40% 30% at 80% 80%, rgba(129,140,248,0.06) 0%, transparent 60%),
    var(--bg-main);
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
[data-theme="light"] .hero-section {
  background:
    radial-gradient(ellipse 70% 70% at 50% -10%, rgba(79,70,229,0.12) 0%, transparent 65%),
    var(--bg-main);
  border-bottom-color: rgba(0,0,0,0.06);
}

/* Noise grain overlay */
.hero-section::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.03;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E");
}

.content {
  position: relative;
  z-index: 10;
  text-align: center;
  padding: 0 24px;
  animation: fadeInUp 0.6s cubic-bezier(0.4, 0, 0.2, 1) both;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* Title with gradient */
.title {
  font-size: 5rem;
  margin-bottom: 1rem;
  font-weight: 800;
  letter-spacing: -0.03em;
  line-height: 1.05;
  /* gradient text */
  background: linear-gradient(135deg, var(--text-header) 40%, #818cf8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* Accent word — indigo glow + shimmer */
.accent {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 20px rgba(99,102,241,0.55));
  animation: accentPulse 3s ease-in-out infinite;
}
@keyframes accentPulse {
  0%, 100% { filter: drop-shadow(0 0 16px rgba(99,102,241,0.5)); }
  50%       { filter: drop-shadow(0 0 28px rgba(129,140,248,0.75)); }
}

.subtitle {
  font-size: 1.2rem;
  margin-bottom: 2.5rem;
  color: var(--text-muted);
  font-weight: 400;
  max-width: 480px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.6;
}

/* CTA Button */
.btn.primary {
  padding: 1rem 2.75rem;
  background: var(--gradient-primary);
  border: none;
  color: white;
  border-radius: 12px;
  font-weight: 700;
  font-size: 1.05rem;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
  box-shadow: 0 8px 28px var(--primary-glow), 0 2px 8px rgba(0,0,0,0.3);
  transition: box-shadow var(--transition-slow), transform var(--transition-base);
  letter-spacing: 0.01em;
}
.btn.primary:hover {
  box-shadow: 0 16px 48px var(--primary-glow-lg), 0 4px 12px rgba(0,0,0,0.4);
  transform: translateY(-3px);
}
.btn.primary:active {
  transform: translateY(-1px);
}

/* ── Novels section ── */
.novels-container {
  max-width: 1200px;
  margin: -28px auto 0;
  padding: 0 24px 100px;
  position: relative;
  z-index: 20;
}

/* Section header with gradient underline */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}
.section-header h2 {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0;
  position: relative;
  display: inline-block;
}
.section-header h2::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 0;
  width: 40px;
  height: 3px;
  border-radius: 2px;
  background: var(--gradient-primary);
}

.view-all {
  color: var(--text-muted);
  text-decoration: none;
  font-weight: 500;
  font-size: 0.9rem;
  transition: color var(--transition-base);
  display: flex;
  align-items: center;
  gap: 4px;
}
.view-all:hover {
  color: var(--btn-plus);
}

/* ── Grid with staggered animation ── */
.novels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1.75rem;
  align-items: stretch;
}
.novels-grid > * {
  animation: fadeInUp 0.5s cubic-bezier(0.4, 0, 0.2, 1) both;
}
.novels-grid > *:nth-child(1)  { animation-delay: 0.05s; }
.novels-grid > *:nth-child(2)  { animation-delay: 0.1s;  }
.novels-grid > *:nth-child(3)  { animation-delay: 0.15s; }
.novels-grid > *:nth-child(4)  { animation-delay: 0.2s;  }
.novels-grid > *:nth-child(5)  { animation-delay: 0.25s; }
.novels-grid > *:nth-child(6)  { animation-delay: 0.3s;  }
.novels-grid > *:nth-child(n+7){ animation-delay: 0.35s; }

.loader {
  text-align: center;
  padding: 60px;
  color: var(--text-muted);
  font-size: 0.95rem;
}

@media (max-width: 640px) {
  .title { font-size: 3.2rem; }
  .subtitle { font-size: 1.05rem; }
  .novels-grid { grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 1.25rem; }
}
</style>

