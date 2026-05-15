<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useLibraryStore } from '@/components/library/libraryStore.ts';
import { useAuthStore } from '@/api/auth.ts';
import { LibraryStatus } from '@/types/library.ts';
import { PrivacyLevel } from '@/types/user.ts'; // Твой Enum приватности

const props = defineProps<{
  novelId: number;
  compact?: boolean;
}>();

const libraryStore = useLibraryStore();
const authStore = useAuthStore();

const isDropdownOpen = ref(false);
const isLoading = ref(false);
const widgetRef = ref<HTMLElement | null>(null); // Ссылка на наш DOM-элемент

// Выбранный уровень приватности (по умолчанию 'Все')
const selectedPrivacy = ref<PrivacyLevel>(PrivacyLevel.NOBODY);

const currentStatus = computed(() => libraryStore.novelStatuses[props.novelId]);

// --- СЛОВАРИ ДЛЯ ИНТЕРФЕЙСА ---
const statusLabels: Record<LibraryStatus, string> = {
  [LibraryStatus.READING]: '📖 Читаю',
  [LibraryStatus.PLANNING]: '🕒 В планах',
  [LibraryStatus.COMPLETED]: '✅ Прочитано',
  [LibraryStatus.DROPPED]: '❌ Брошено',
};

const privacyLabels: Record<PrivacyLevel, string> = {
  [PrivacyLevel.EVERYONE]: '🌍 Видят все',
  [PrivacyLevel.FOLLOWERS]: '👥 Только подписчики',
  [PrivacyLevel.FRIENDS]: '🤝 Только друзья',
  [PrivacyLevel.BEST_FRIENDS]: '⭐ Близкие друзья',
  [PrivacyLevel.NOBODY]: '🔒 Только я',
};

const buttonText = computed(() => {
  if (isLoading.value) return '⏳ Загрузка...';
  if (currentStatus.value) return statusLabels[currentStatus.value];
  return '+ В библиотеку';
});

// --- ЛОГИКА СОХРАНЕНИЯ ---
const changeStatus = async (newStatus: LibraryStatus) => {
  if (!authStore.isAuthenticated) {
    authStore.showAuthModal = true;
    isDropdownOpen.value = false;
    return;
  }

  isLoading.value = true;
  isDropdownOpen.value = false;

  try {
    // Передаем статус и выбранную приватность!
    await libraryStore.updateStatus(props.novelId, newStatus, selectedPrivacy.value);
  } finally {
    isLoading.value = false;
  }
};

// Если пользователь просто меняет приватность у УЖЕ добавленной книги
const handlePrivacyChange = async () => {
  if (currentStatus.value) {
    isLoading.value = true;
    try {
      await libraryStore.updateStatus(props.novelId, currentStatus.value, selectedPrivacy.value);
    } finally {
      isLoading.value = false;
    }
  }
};

const removeStatus = async () => {
  isLoading.value = true;
  isDropdownOpen.value = false;
  try {
    await libraryStore.removeStatus(props.novelId);
  } finally {
    isLoading.value = false;
  }
};

// --- ЗАКРЫТИЕ ПО КЛИКУ ВНЕ МЕНЮ ---
const handleClickOutside = (event: MouseEvent) => {
  // Если кликнули не внутри нашего виджета - закрываем меню
  if (widgetRef.value && !widgetRef.value.contains(event.target as Node)) {
    isDropdownOpen.value = false;
  }
};

onMounted(() => document.addEventListener('click', handleClickOutside));
onUnmounted(() => document.removeEventListener('click', handleClickOutside));
</script>

<template>
  <div class="library-widget" ref="widgetRef">

    <button
      class="main-btn"
      :class="{ 'is-active': currentStatus, 'is-compact': compact }"
      :disabled="isLoading"
      @click="isDropdownOpen = !isDropdownOpen"
    >
      {{ buttonText }}
    </button>

    <Transition name="fade-slide">
      <div v-if="isDropdownOpen" class="dropdown">

        <div class="dropdown-section">
          <span class="section-title">Статус</span>
          <button
            v-for="(label, statusKey) in statusLabels"
            :key="statusKey"
            class="dropdown-item"
            :class="{ 'selected': currentStatus === statusKey }"
            @click="changeStatus(statusKey as LibraryStatus)"
          >
            {{ label }}
          </button>
        </div>

        <div class="divider"></div>

        <div class="dropdown-section">
          <span class="section-title">Кто это увидит?</span>
          <select
            class="privacy-select"
            v-model="selectedPrivacy"
            @change="handlePrivacyChange"
          >
            <option
              v-for="(label, privacyKey) in privacyLabels"
              :key="privacyKey"
              :value="privacyKey"
            >
              {{ label }}
            </option>
          </select>
        </div>

        <template v-if="currentStatus">
          <div class="divider"></div>
          <button class="dropdown-item remove-btn" @click="removeStatus">
            🗑️ Удалить из списка
          </button>
        </template>

      </div>
    </Transition>
  </div>
</template>

<style scoped>
.library-widget {
  position: relative;
  display: inline-block;
  font-family: inherit;
}

.main-btn {
  padding: 8px 16px;
  background-color: var(--btn-plus);
  color: #ffffff;
  border: 1px solid transparent;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 150px;
}

.main-btn:disabled {
  opacity: 0.7;
  cursor: wait;
}

.main-btn.is-active {
  background-color: var(--bg-dropdown);
  color: var(--text-header);
  border: 1px solid var(--border-color);
}

.main-btn:hover:not(:disabled) {
  filter: brightness(1.1);
}

.dropdown {
  position: absolute;
  top: calc(100% + 5px);
  left: 0;
  background-color: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  min-width: 200px; /* Сделал чуть шире для селекта */
  box-shadow: 0 4px 15px var(--shadow-color);
  z-index: 50;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 5px 0; /* Отступы сверху и снизу для красоты */
}

/* Стили для секций внутри меню */
.dropdown-section {
  display: flex;
  flex-direction: column;
  padding: 5px 0;
}

.section-title {
  font-size: 0.75rem;
  text-transform: uppercase;
  color: var(--text-muted);
  padding: 4px 15px;
  letter-spacing: 0.5px;
  font-weight: bold;
}

.dropdown-item {
  background: none;
  border: none;
  padding: 8px 15px;
  color: var(--text-header);
  text-align: left;
  cursor: pointer;
  font-size: 0.95rem;
  transition: background-color 0.2s;
}

.dropdown-item:hover {
  background-color: var(--hover-dropdowb);
}

.dropdown-item.selected {
  background-color: var(--border-subtle);
  color: var(--btn-plus);
  font-weight: bold;
}

/* Стили для селекта приватности */
.privacy-select {
  margin: 5px 15px;
  padding: 8px;
  background-color: var(--bg-main);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-family: inherit;
  font-size: 0.9rem;
  cursor: pointer;
  outline: none;
}
.privacy-select:focus {
  border-color: var(--btn-plus);
}

.divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 4px 0;
}

.remove-btn {
  color: #e74c3c;
  padding: 10px 15px;
}

.remove-btn:hover {
  background-color: rgba(231, 76, 60, 0.1);
  color: #ff6b6b;
}

.main-btn.is-compact {
  padding: 6px 10px;
  min-width: auto; /* Убираем жесткую ширину */
  font-size: 0.85rem;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
