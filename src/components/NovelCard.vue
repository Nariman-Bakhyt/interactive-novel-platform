<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import type { NovelResponseDto } from "@/types/novel.ts";
import LibraryButton from "@/components/library/LibraryButton.vue";

const props = defineProps<{
  novel: NovelResponseDto;
  isMobile: boolean; // Получаем из родителя, чтобы не вешать 20 слушателей окна
}>();

const emit = defineEmits<{
  (e: 'click', id: number): void
}>();

const router = useRouter();

// Локальное состояние для конкретной карточки
const isHovered = ref(false);
let hoverTimeout: number | null = null;
let closeTimeout: number | null = null;

const handleCardClick = () => {
  emit('click', props.novel.id);
};
const handleMouseEnter = () => {
  if (props.isMobile) return;
  if (closeTimeout) clearTimeout(closeTimeout);
  hoverTimeout = window.setTimeout(() => {
    isHovered.value = true;
  }, 300);
};

const handleMouseLeave = () => {
  if (hoverTimeout) clearTimeout(hoverTimeout);
  closeTimeout = window.setTimeout(() => {
    isHovered.value = false;
  }, 400);
};

const handlePopoverEnter = () => {
  if (closeTimeout) clearTimeout(closeTimeout);
};

const toggleInfo = (event: Event) => {
  event.stopPropagation();
  isHovered.value = !isHovered.value;
};
</script>

<template>
  <div class="novel-card-wrapper" @mouseleave="handleMouseLeave">

    <div class="novel-card" @mouseenter="handleMouseEnter" @click="handleCardClick">
      <div class="cover-wrapper">
        <img :src="novel.coverUrl || 'http://127.0.0.1:9000/interactive-novel-assets/covers/default-cover.png'" :alt="novel.title" />
        <button class="info-trigger" @click="toggleInfo"><i>i</i></button>
      </div>
      <div class="novel-info">
        <h3>{{ novel.title }}</h3>
        <p>{{ novel.description }}</p>
      </div>
    </div>

    <Transition name="slide-fade">
      <div v-if="isHovered" class="novel-popover" @mouseenter="handlePopoverEnter">
        <div class="popover-content">

          <div class="pop-header">
            <h4>{{ novel.title }}</h4>
            <button v-if="isMobile" class="close-pop" @click="toggleInfo">×</button>
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
          </div>

          <p class="pop-desc">{{ novel.description }}</p>

          <div class="pop-footer">
            <span :class="['status-badge', novel.status?.toLowerCase()]">{{ novel.status }}</span>

            <LibraryButton :novel-id="novel.id" :compact="true" />

            <button class="btn-read-now" @click="handleCardClick">Читать</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.novel-card-wrapper { position: relative; display: flex; height: 100%; z-index: 1; }
.novel-card-wrapper:hover { z-index: 50; }
.novel-card { display: flex; flex-direction: column; width: 100%; background: var(--bg-dropdown); border-radius: 16px; overflow: hidden; border: 1px solid var(--border-subtle); transition: transform 0.3s ease; cursor: pointer; }
.novel-card:hover { transform: translateY(-10px); }

.cover-wrapper { position: relative; height: 350px; }
.cover-wrapper img { width: 100%; height: 100%; object-fit: cover; }
.info-trigger { position: absolute; top: 10px; right: 10px; width: 32px; height: 32px; border-radius: 50%; background: rgba(0, 0, 0, 0.7); border: 1px solid rgba(255, 255, 255, 0.3); color: white; display: flex; justify-content: center; align-items: center; cursor: pointer; backdrop-filter: blur(5px); transition: background 0.2s; }
.info-trigger:hover { background: rgba(0, 0, 0, 0.9); }

.novel-info { padding: 1.5rem; display: flex; flex-direction: column; flex-grow: 1; }
.novel-info h3 { margin-bottom: 0.5rem; font-size: 1.2rem; color: var(--text-header); }
.novel-info p { font-size: 0.9rem; color: var(--text-muted); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

/* --- ВСПЛЫВАЮЩЕЕ ОКНО (POPOVER) --- */
.novel-popover { position: absolute; top: 0; left: 105%; width: 320px; background: var(--bg-dropdown); border: 1px solid var(--border-color); border-radius: 12px; box-shadow: 15px 10px 40px var(--shadow-color); z-index: 100; padding: 20px; pointer-events: all; }
.novel-popover::before { content: ''; position: absolute; top: 0; left: -30px; width: 35px; height: 100%; background: transparent; }
.novel-popover::after { content: ''; position: absolute; top: 30px; left: -10px; border-width: 10px 10px 10px 0; border-style: solid; border-color: transparent var(--bg-dropdown) transparent transparent; }

.pop-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.pop-header h4 { margin: 0; font-size: 1.2rem; color: var(--text-header); }
.close-pop { background: none; border: none; color: var(--text-muted); font-size: 1.5rem; cursor: pointer; line-height: 1; }

.pop-stats { display: flex; gap: 15px; margin-bottom: 12px; font-size: 0.85rem; color: var(--text-header); font-weight: 600; }
.stat-item { display: flex; align-items: center; gap: 4px; }
.rating { color: #f1c40f; }

.pop-metadata { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
.chips-row { display: flex; flex-wrap: wrap; gap: 6px; }
.pop-chip { font-size: 0.7rem; padding: 2px 8px; border-radius: 4px; background: var(--bg-main); border: 1px solid var(--border-color); }
.pop-chip.genre { border-color: var(--btn-plus); color: var(--btn-plus); }

.pop-desc { font-size: 0.8rem; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 5; -webkit-box-orient: vertical; overflow: hidden; margin-bottom: 15px; color: var(--text-muted); }
.pop-footer { display: flex; justify-content: space-between; align-items: center; gap: 10px; }

/* --- ЭЛЕМЕНТЫ ВНУТРИ POPOVER --- */
.status-badge { font-size: 0.75rem; padding: 4px 8px; border-radius: 4px; font-weight: bold; }
.status-badge.completed { background: rgba(46, 204, 113, 0.2); color: #2ecc71; }
.status-badge.in_progress { background: rgba(52, 152, 219, 0.2); color: #3498db; }
.status-badge.draft { background: rgba(149, 165, 166, 0.2); color: #95a5a6; }

.btn-read-now { padding: 6px 14px; background: #6366f1; border: none; color: white; border-radius: 6px; cursor: pointer; font-size: 0.85rem; font-weight: bold; transition: background 0.2s; white-space: nowrap; }
.btn-read-now:hover { background: #4f46e5; }

/* --- АДАПТИВ И АНИМАЦИИ --- */
@media (max-width: 768px) {
  .novel-popover { position: fixed; top: 50%; left: 50% !important; transform: translate(-50%, -50%); width: 85%; max-height: 70vh; overflow-y: auto; }
  .novel-popover::before, .novel-popover::after { display: none; }
}

.slide-fade-enter-active, .slide-fade-leave-active { transition: all 0.3s ease; }
.slide-fade-enter-from, .slide-fade-leave-to { opacity: 0; transform: translateX(10px); }
</style>
