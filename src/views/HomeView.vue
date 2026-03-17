<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getNewNovels } from "@/api/novelService.ts";
import type { NovelResponseDto } from "@/types/novel.ts";
import { useRouter } from "vue-router";

const novels = ref<NovelResponseDto[]>([]);
const isLoading = ref(true);
const router = useRouter();

const hoveredNovelId = ref<number | null>(null);
const isMobile = ref(false);
let hoverTimeout: number | null = null;
let closeTimeout: number | null = null;

const handleCardClick = (id: number) => {
  router.push({ name: 'NovelDetail', params: { id: id.toString() } });
};

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

onMounted(() => {
  fetchNovels();
  isMobile.value = window.innerWidth <= 768;
  window.addEventListener('resize', () => {
    isMobile.value = window.innerWidth <= 768;
  });
});

const handleMouseEnter = (id: number) => {
  if (isMobile.value) return;
  if (closeTimeout) clearTimeout(closeTimeout);
  hoverTimeout = window.setTimeout(() => {
    hoveredNovelId.value = id;
  }, 300);
};

const handleMouseLeave = () => {
  if (hoverTimeout) clearTimeout(hoverTimeout);
  closeTimeout = window.setTimeout(() => {
    hoveredNovelId.value = null;
  }, 400);
};

const handlePopoverEnter = () => {
  if (closeTimeout) clearTimeout(closeTimeout);
};

const toggleInfo = (id: number, event: Event) => {
  event.stopPropagation();
  hoveredNovelId.value = hoveredNovelId.value === id ? null : id;
};
</script>

<template>
  <div class="page-wrapper">
    <section class="hero-section">
      <div class="overlay"></div>
      <div class="content">
        <h1 class="title">Read<span class="accent">Hub</span></h1>
        <p class="subtitle">Погружайся в миры, созданные сообществом.</p>
        <button class="btn primary">Начать читать</button>
      </div>
    </section>

    <section class="novels-container">
      <div class="section-header">
        <h2>Свежие новинки</h2>
        <router-link to="/novels" class="view-all">Смотреть все →</router-link>
      </div>

      <div v-if="isLoading" class="loader">Загрузка...</div>

      <div v-else class="novels-grid">
        <div
          v-for="novel in novels"
          :key="novel.id"
          class="novel-card-wrapper"
          @mouseleave="handleMouseLeave"
        >
          <div
            class="novel-card"
            @mouseenter="handleMouseEnter(novel.id)"
            @click="handleCardClick(novel.id)"
          >
            <div class="cover-wrapper">
              <img :src="novel.coverUrl || 'http://127.0.0.1:9000/interactive-novel-assets/Cover/default-cover.png'" :alt="novel.title" />
              <button class="info-trigger" @click="toggleInfo(novel.id, $event)">
                <i>i</i>
              </button>
            </div>
            <div class="novel-info">
              <h3>{{ novel.title }}</h3>
              <p>{{ novel.description }}</p>
            </div>
          </div>

          <Transition name="slide-fade">
            <div
              v-if="hoveredNovelId === novel.id"
              class="novel-popover"
              @mouseenter="handlePopoverEnter"
            >
              <div class="popover-content">
                <div class="pop-header">
                  <h4>{{ novel.title }}</h4>
                  <button v-if="isMobile" class="close-pop" @click="toggleInfo(novel.id, $event)">×</button>
                </div>

                <div class="pop-stats">
                  <span class="stat-item rating">⭐ {{ novel.ratingCount > 0 ? (novel.totalScore / novel.ratingCount).toFixed(1) : '0.0' }}</span>
                  <span class="stat-item views">👁‍🗨 {{ novel.viewCount || 0 }}</span>
                  <span class="stat-item chapters">📚 {{ novel.chapterCount || 0 }} гл.</span>
                </div>

                <div class="pop-metadata">
                  <div class="chips-row">
                    <span v-for="genre in novel.genres?.slice(0, 3)" :key="genre.id" class="pop-chip genre">{{ genre.name }}</span>
                  </div>
                  <div class="chips-row">
                    <span v-for="tag in novel.tags?.slice(0, 3)" :key="tag.id" class="pop-chip tag">#{{ tag.name }}</span>
                  </div>
                </div>

                <p class="pop-desc">{{ novel.description }}</p>

                <div class="pop-footer">
                  <span :class="['status-badge', novel.status?.toLowerCase()]">{{ novel.status }}</span>
                  <button class="btn-read-now" @click="handleCardClick(novel.id)">Читать</button>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* Layout */
.page-wrapper { background-color: var(--bg-main); min-height: 100vh; color: var(--text-header); }
.novels-container { max-width: 1200px; margin: -50px auto 0; padding: 0 20px 100px; position: relative; z-index: 20; }
.novels-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 2rem; align-items: stretch; }

/* Hero Section */
.hero-section { position: relative; height: 80vh; display: flex; justify-content: center; align-items: center; background: url('https://images.unsplash.com/photo-1519681393784-d120267933ba?q=80&w=2070&auto=format&fit=crop') center/cover; }
.overlay { position: absolute; inset: 0; background: linear-gradient(to bottom, rgba(15, 23, 42, 0.4), var(--bg-main)); }
.content { position: relative; z-index: 10; text-align: center; }
.accent { color: #6366f1; }
.title { font-size: 4rem; margin-bottom: 1rem; }

/* Novel Card */
.novel-card-wrapper { position: relative; display: flex; height: 100%; z-index: 1; }
.novel-card-wrapper:hover { z-index: 50; }
.novel-card { display: flex; flex-direction: column; width: 100%; background: var(--bg-dropdown); border-radius: 16px; overflow: hidden; border: 1px solid var(--border-subtle); transition: transform 0.3s ease; cursor: pointer; }
.novel-card:hover { transform: translateY(-10px); }

.cover-wrapper { position: relative; height: 350px; }
.cover-wrapper img { width: 100%; height: 100%; object-fit: cover; }
.info-trigger { position: absolute; top: 10px; right: 10px; width: 32px; height: 32px; border-radius: 50%; background: rgba(0, 0, 0, 0.7); border: 1px solid rgba(255, 255, 255, 0.3); color: white; display: flex; justify-content: center; align-items: center; cursor: pointer; backdrop-filter: blur(5px); }

.novel-info { padding: 1.5rem; display: flex; flex-direction: column; flex-grow: 1; }
.novel-info h3 { margin-bottom: 0.5rem; font-size: 1.2rem; color: var(--text-header); }
.novel-info p { font-size: 0.9rem; color: var(--text-muted); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

/* Popover */
.novel-popover { position: absolute; top: 0; left: 105%; width: 320px; background: var(--bg-dropdown); border: 1px solid var(--border-color); border-radius: 12px; box-shadow: 15px 10px 40px rgba(0, 0, 0, 0.6); z-index: 100; padding: 20px; pointer-events: all; }
.novel-popover::before { content: ''; position: absolute; top: 0; left: -30px; width: 35px; height: 100%; background: transparent; }
.novel-popover::after { content: ''; position: absolute; top: 30px; left: -10px; border-width: 10px 10px 10px 0; border-style: solid; border-color: transparent var(--bg-dropdown) transparent transparent; }

.pop-stats { display: flex; gap: 15px; margin-bottom: 12px; font-size: 0.85rem; color: var(--text-header); font-weight: 600; }
.stat-item { display: flex; align-items: center; gap: 4px; }
.rating { color: #f1c40f; }

.pop-metadata { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
.chips-row { display: flex; flex-wrap: wrap; gap: 6px; }
.pop-chip { font-size: 0.7rem; padding: 2px 8px; border-radius: 4px; background: var(--bg-main); border: 1px solid var(--border-color); }
.pop-chip.genre { border-color: #42b883; color: #42b883; }
.pop-chip.tag { color: var(--text-muted); }

.pop-desc { font-size: 0.8rem; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 5; -webkit-box-orient: vertical; overflow: hidden; margin-bottom: 15px; }
.pop-footer { display: flex; justify-content: space-between; align-items: center; }

/* Status Badges */
.status-badge { font-size: 0.75rem; padding: 4px 8px; border-radius: 4px; }
.status-badge.completed { background: rgba(46, 204, 113, 0.2); color: #2ecc71; }
.status-badge.in_progress { background: rgba(52, 152, 219, 0.2); color: #3498db; }
.status-badge.draft { background: rgba(149, 165, 166, 0.2); color: #95a5a6; }

/* UI Elements */
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
.btn.primary { padding: 1rem 2rem; background: #6366f1; border: none; color: white; border-radius: 8px; font-weight: bold; cursor: pointer; }
.btn-read-now { padding: 6px 14px; background: #6366f1; border: none; color: white; border-radius: 6px; cursor: pointer; font-size: 0.85rem; }
.close-pop { background: none; border: none; color: var(--text-header); font-size: 1.5rem; cursor: pointer; }
.loader { text-align: center; padding: 50px; color: var(--text-muted); }

@media (max-width: 768px) {
  .novel-popover { position: fixed; top: 50%; left: 50% !important; transform: translate(-50%, -50%); width: 85%; max-height: 70vh; }
  .novel-popover::before, .novel-popover::after { display: none; }
}

.slide-fade-enter-active, .slide-fade-leave-active { transition: all 0.3s ease; }
.slide-fade-enter-from, .slide-fade-leave-to { opacity: 0; transform: translateX(10px); }
</style>
