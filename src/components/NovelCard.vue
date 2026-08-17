<script setup lang="ts">
import {ref} from 'vue';
import type {NovelResponseDto} from "@/types/novel.ts";
import LibraryButton from "@/components/library/LibraryButton.vue";
import {DEFAULT_COVER} from "@/utils/media.ts";

const props = defineProps<{
  novel: NovelResponseDto;
  isMobile: boolean; 
}>();

const emit = defineEmits<{
  (e: 'click', id: number): void
}>();



const isHovered = ref(false);
const popoverPosition = ref<'right' | 'left'>('right');
const wrapperRef = ref<HTMLElement | null>(null);
let hoverTimeout: number | null = null;
let closeTimeout: number | null = null;

const handleCardClick = () => {
  emit('click', props.novel.id);
};
const handleMouseEnter = () => {
  if (props.isMobile) return;
  if (closeTimeout) clearTimeout(closeTimeout);
  
  if (wrapperRef.value) {
    const rect = wrapperRef.value.getBoundingClientRect();
    if (window.innerWidth - rect.right < 320 && rect.left > 320) {
      popoverPosition.value = 'left';
    } else {
      popoverPosition.value = 'right';
    }
  }

  hoverTimeout = window.setTimeout(() => {
    isHovered.value = true;
  }, 300);
};

const handleMouseLeave = () => {
  if (props.isMobile) return;
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
  if (!isHovered.value && wrapperRef.value) {
    const rect = wrapperRef.value.getBoundingClientRect();
    if (window.innerWidth - rect.right < 320 && rect.left > 320) {
      popoverPosition.value = 'left';
    } else {
      popoverPosition.value = 'right';
    }
  }
  isHovered.value = !isHovered.value;
};
</script>

<template>
  <div class="novel-card-wrapper" ref="wrapperRef" @mouseleave="handleMouseLeave">

    <div class="novel-card" @mouseenter="handleMouseEnter" @click="handleCardClick">
      <div class="cover-wrapper">
        <img :src="novel.coverUrl || DEFAULT_COVER" :alt="novel.title" />
        <button class="info-trigger" @click="toggleInfo"><i>i</i></button>
      </div>
      <div class="novel-info">
        <h3>{{ novel.title }}</h3>
        <p>{{ novel.description }}</p>
      </div>
    </div>

    <Transition name="slide-fade">
      <div v-if="isHovered" :class="['novel-popover', popoverPosition]" @mouseenter="handlePopoverEnter">
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
/* ── Wrapper & z-index management ── */
.novel-card-wrapper {
  position: relative;
  display: flex;
  height: 100%;
  z-index: 1;
}
.novel-card-wrapper:hover { z-index: 50; }

/* ── Card ── */
.novel-card {
  display: flex;
  flex-direction: column;
  width: 100%;
  background: var(--bg-dropdown);
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  transition: transform 0.3s cubic-bezier(0.4,0,0.2,1),
              box-shadow 0.3s cubic-bezier(0.4,0,0.2,1),
              border-color 0.3s ease;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0,0,0,0.3);
}
.novel-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0,0,0,0.5), 0 0 0 1px var(--btn-plus);
  border-color: var(--btn-plus);
}

/* ── Cover ── */
.cover-wrapper {
  position: relative;
  height: 320px;
  overflow: hidden;
}
.cover-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.4,0,0.2,1);
}
.novel-card:hover .cover-wrapper img {
  transform: scale(1.07);
}
/* subtle gradient overlay on cover */
.cover-wrapper::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.45) 0%, transparent 60%);
  pointer-events: none;
}

/* ── Info trigger button ── */
.info-trigger {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(0,0,0,0.55);
  border: 1px solid rgba(255,255,255,0.2);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  backdrop-filter: blur(6px);
  transition: background 0.2s, transform 0.2s;
}
.info-trigger:hover {
  background: rgba(99,102,241,0.7);
  transform: scale(1.12);
}

/* ── Card info section ── */
.novel-info {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}
.novel-info h3 {
  margin: 0 0 0.5rem;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-header);
  line-height: 1.3;
  /* clamp to 2 lines */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.novel-info p {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-muted);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ═══════════════════════════════════════
   Popover
═══════════════════════════════════════ */
.novel-popover {
  position: absolute;
  top: 0;
  left: calc(100% + 12px); /* gap instead of 105% */
  width: 300px;
  /* glassmorphism */
  background: var(--bg-dropdown);
  backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 14px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.5), 0 0 0 1px rgba(99,102,241,0.12);
  z-index: 200;
  padding: 18px;
  pointer-events: all;
  /* prevent going off-screen on right */
  max-width: min(300px, calc(100vw - 32px));
}

/* invisible hover bridge from card to popover */
.novel-popover::before {
  content: '';
  position: absolute;
  top: 0;
  left: -16px;
  width: 20px;
  height: 100%;
  background: transparent;
}
.novel-popover.right::after {
  content: '';
  position: absolute;
  top: 28px;
  left: -8px;
  border-width: 8px 8px 8px 0;
  border-style: solid;
  border-color: transparent var(--bg-dropdown) transparent transparent;
}

/* ── Popover Left Position ── */
.novel-popover.left {
  left: auto;
  right: calc(100% + 12px);
}
.novel-popover.left::before {
  left: auto;
  right: -16px;
}
.novel-popover.left::after {
  content: '';
  position: absolute;
  top: 28px;
  right: -8px;
  left: auto;
  border-width: 8px 0 8px 8px;
  border-style: solid;
  border-color: transparent transparent transparent var(--bg-dropdown);
}

/* ── Popover Header ── */
.pop-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}
.pop-header h4 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-header);
  line-height: 1.3;
  /* clamp to 2 lines */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.close-pop {
  flex-shrink: 0;
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.4rem;
  cursor: pointer;
  line-height: 1;
  transition: color 0.2s;
  margin-left: 8px;
}
.close-pop:hover { color: var(--text-header); }

/* ── Stats row ── */
.pop-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 0.82rem;
  color: var(--text-header);
  font-weight: 600;
}
.stat-item { display: flex; align-items: center; gap: 4px; }
.rating { color: #f59e0b; }

/* ── Genre chips ── */
.pop-metadata { margin-bottom: 10px; }
.chips-row { display: flex; flex-wrap: wrap; gap: 5px; }
.pop-chip {
  font-size: 0.72rem;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-muted);
}
.pop-chip.genre {
  border-color: rgba(99,102,241,0.3);
  color: #818cf8;
  background: rgba(99,102,241,0.08);
}

/* ── Description ── */
.pop-desc {
  font-size: 0.875rem;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 14px;
  color: var(--text-muted);
}

/* ─────────────────────────────────────
   pop-footer FIX: wrap so buttons never overflow
───────────────────────────────────── */
.pop-footer {
  display: flex;
  flex-wrap: wrap;       /* ← allows wrap when space is tight */
  align-items: center;
  gap: 8px;
}

/* Status badge stays on its own row if needed */
.status-badge {
  font-size: 0.62rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 3px 8px;
  border-radius: 999px;
  font-weight: 700;
  white-space: nowrap;
  flex-shrink: 0;
}
.status-badge.completed  { background: rgba(16,185,129,0.1);  color: #10b981; border: 1px solid rgba(16,185,129,0.2); }
.status-badge.in_progress{ background: rgba(99,102,241,0.1);  color: #818cf8; border: 1px solid rgba(99,102,241,0.2); }
.status-badge.draft      { background: rgba(161,161,170,0.1); color: #a1a1aa; border: 1px solid rgba(161,161,170,0.2); }

/* Library button — compact override */
.pop-footer :deep(.main-btn.is-compact) {
  padding: 6px 12px;
  font-size: 0.82rem;
  border-radius: 8px;
  white-space: nowrap;
  flex-shrink: 0;
}

/* Read now button */
.btn-read-now {
  flex-shrink: 0;
  padding: 7px 14px;
  background: linear-gradient(135deg, #6366f1, #818cf8);
  border: none;
  color: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.82rem;
  font-weight: 600;
  white-space: nowrap;
  transition: box-shadow 0.2s, transform 0.2s;
  box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}
.btn-read-now:hover {
  box-shadow: 0 6px 20px rgba(99,102,241,0.5);
  transform: translateY(-1px);
}

/* ── Mobile: centered modal ── */
@media (max-width: 768px) {
  .novel-popover {
    position: fixed;
    top: 50%;
    left: 50% !important;
    transform: translate(-50%, -50%) !important;
    width: 88%;
    max-width: 360px;
    max-height: 75vh;
    overflow-y: auto;
  }
  .novel-popover::before,
  .novel-popover::after { display: none; }
}

/* ── Animations ── */
.slide-fade-enter-active,
.slide-fade-leave-active { transition: opacity 0.2s ease, transform 0.2s cubic-bezier(0.4,0,0.2,1); }
.slide-fade-enter-from,
.slide-fade-leave-to { opacity: 0; transform: translateX(8px); }
</style>
