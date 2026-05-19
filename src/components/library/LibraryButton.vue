<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue';
import {useLibraryStore} from '@/components/library/libraryStore.ts';
import {useAuthStore} from '@/api/auth.ts';
import {LibraryStatus} from '@/types/library.ts';
import {PrivacyLevel} from '@/types/user.ts'; 

const props = defineProps<{
  novelId: number;
  compact?: boolean;
}>();

const libraryStore = useLibraryStore();
const authStore = useAuthStore();

const isDropdownOpen = ref(false);
const isLoading = ref(false);
const widgetRef = ref<HTMLElement | null>(null); 


const selectedPrivacy = ref<PrivacyLevel>(PrivacyLevel.NOBODY);

const currentStatus = computed(() => libraryStore.novelStatuses[props.novelId]);


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


const changeStatus = async (newStatus: LibraryStatus) => {
  if (!authStore.isAuthenticated) {
    authStore.showAuthModal = true;
    isDropdownOpen.value = false;
    return;
  }

  isLoading.value = true;
  isDropdownOpen.value = false;

  try {
    
    await libraryStore.updateStatus(props.novelId, newStatus, selectedPrivacy.value);
  } finally {
    isLoading.value = false;
  }
};


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


const handleClickOutside = (event: MouseEvent) => {
  
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
            <span class="icon">🗑️</span> Удалить из списка
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
  padding: 8px 20px;
  background-color: var(--btn-plus);
  color: #ffffff;
  border: 1px solid transparent;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
  min-width: 160px;
  font-size: 0.95rem;
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
  transform: translateY(-1px);
}
.main-btn:not(.is-active):hover:not(:disabled) {
  background-color: var(--btn-plus-hover);
}
.main-btn.is-active:hover:not(:disabled) {
  background-color: var(--hover-dropdowb);
  border-color: var(--text-muted);
}

.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  background-color: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  min-width: 220px;
  box-shadow: 0 10px 30px var(--shadow-color);
  z-index: 50;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 8px 0;
}


.dropdown-section {
  display: flex;
  flex-direction: column;
  padding: 4px 0;
}

.section-title {
  font-size: 0.75rem;
  text-transform: uppercase;
  color: var(--text-muted);
  padding: 4px 16px;
  letter-spacing: 0.05em;
  font-weight: 700;
}

.dropdown-item {
  background: none;
  border: none;
  padding: 10px 16px;
  color: var(--text-header);
  text-align: left;
  cursor: pointer;
  font-size: 0.95rem;
  transition: background 0.2s;
  font-weight: 500;
}

.dropdown-item:hover {
  background-color: var(--hover-dropdowb);
}

.dropdown-item.selected {
  background-color: rgba(99, 102, 241, 0.1);
  color: var(--btn-plus);
  font-weight: 600;
}


.privacy-select {
  margin: 4px 16px 8px;
  padding: 8px 12px;
  background-color: var(--bg-main);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-family: inherit;
  font-size: 0.9rem;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
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
  color: #ef4444; 
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.remove-btn:hover {
  background-color: rgba(239, 68, 68, 0.1);
}

.main-btn.is-compact {
  padding: 8px 12px;
  min-width: auto;
  font-size: 0.9rem;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
</style>
