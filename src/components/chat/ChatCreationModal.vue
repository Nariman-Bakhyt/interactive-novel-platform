<script setup lang="ts">
import {ref, computed, watch, onMounted, inject, onUnmounted} from 'vue';
import { useSocialStore } from "@/components/social/socialStore.ts";
import { useMessengerStore } from "@/components/chat/messengerStore.ts";
import { useToastStore } from "@/components/toast/toastStore.ts";
import { searchUsers } from "@/api/profileService.ts";
import { getCloseFriends, getFollowers, getFollowing, getFriends } from "@/api/socialService.ts";

const emit = defineEmits(['close', 'add-members']);
const socialStore = useSocialStore();
const messengerStore = useMessengerStore();
const toastStore = useToastStore();
const openUserMenu = inject('openUserMenu') as (event: MouseEvent, userId: number, username: string) => void;

const props = defineProps<{
  purpose?: 'CREATE' | 'ADD_MEMBERS'; // НОВЫЙ ПРОП
  excludeIds?: number[]; // Чтобы не показывать тех, кто уже в группе
}>();

// --- РЕЖИМЫ И СОСТОЯНИЕ ГРУППЫ ---
const mode = ref<'PRIVATE' | 'GROUP'>(props.purpose === 'ADD_MEMBERS' ? 'GROUP' : 'PRIVATE');
const groupName = ref('');
const selectedUserIds = ref<Set<number>>(new Set());

const groupAvatarFile = ref<File | null>(null);
const groupAvatarPreview = ref<string>('');
const fileInput = ref<HTMLInputElement | null>(null);

// --- ПОИСК И ВКЛАДКИ ---
const searchQuery = ref('');
const activeTab = ref('friends');

const tabs = [
  { id: 'friends', label: 'Друзья', fn: getFriends },
  { id: 'close', label: 'Близкие', fn: async () => ({ content: await getCloseFriends(), last: true }) },
  { id: 'followers', label: 'Подписчики', fn: getFollowers },
  { id: 'following', label: 'Подписки', fn: getFollowing }
];

const tabData = ref({ items: [] as any[], page: 0, isLast: false, isLoading: false });
const globalData = ref({ items: [] as any[], page: 0, isLast: false, isLoading: false });

onMounted(async () => {
  if (!socialStore.isLoaded) await socialStore.fetchSocialGraph();
  await loadTabData(true);
});

onUnmounted(() => {
  if (groupAvatarPreview.value.startsWith('blob:')) URL.revokeObjectURL(groupAvatarPreview.value);
});

const triggerFileUpload = () => fileInput.value?.click();

const handleAvatarUpload = (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  if (groupAvatarPreview.value.startsWith('blob:')) URL.revokeObjectURL(groupAvatarPreview.value);
  groupAvatarPreview.value = URL.createObjectURL(file);
  groupAvatarFile.value = file;
};


const switchMode = (newMode: 'PRIVATE' | 'GROUP') => {
  if (props.purpose === 'ADD_MEMBERS') return; // Запрещаем смену режима
  mode.value = newMode;
  selectedUserIds.value.clear();
  if (newMode === 'PRIVATE') { groupName.value = ''; groupAvatarFile.value = null; groupAvatarPreview.value = ''; }
};

const setTab = async (tabId: string) => { activeTab.value = tabId; searchQuery.value = ''; await loadTabData(true); };

const loadTabData = async (reset = false) => {
  if (reset) tabData.value = { items: [], page: 0, isLast: false, isLoading: false };
  if (tabData.value.isLoading || tabData.value.isLast) return;
  tabData.value.isLoading = true;
  try {
    const tab = tabs.find(t => t.id === activeTab.value);
    if (tab) {
      const res = activeTab.value === 'close' ? await tab.fn() : await tab.fn(tabData.value.page, 20);
      const newItems = res.content || [];
      tabData.value.items.push(...newItems);
      tabData.value.isLast = res.last ?? true;
      if (!tabData.value.isLast) tabData.value.page++;
    }
  } finally { tabData.value.isLoading = false; }
};

const loadGlobalData = async (force = false) => {
  if (!force && (globalData.value.isLoading || globalData.value.isLast)) return;
  globalData.value.isLoading = true;
  try {
    const res = await searchUsers(searchQuery.value, globalData.value.page, 20);
    if (res.content && res.content.length > 0) {
      const filtered = res.content.filter((u: any) => !socialStore.isBlocked(u.id));
      globalData.value.items.push(...filtered);
      globalData.value.isLast = (res.page.number + 1) >= res.page.totalPages;
      if (!globalData.value.isLast) globalData.value.page = res.page.number + 1;
    } else { globalData.value.isLast = true; }
  } finally { globalData.value.isLoading = false; }
};

const isAllowedUser = (user: any) => {
  const id = getUserId(user);
  return !socialStore.isBlocked(id) && !(props.excludeIds?.includes(id)); // Скрываем тех, кто уже в чате
};

const filteredLocalItems = computed(() => {
  const q = searchQuery.value.toLowerCase().trim();
  return tabData.value.items.filter(u =>
    isAllowedUser(u) && (!q || u.username.toLowerCase().includes(q))
  );
});

const isGlobalSearchActive = computed(() => searchQuery.value.trim().length >= 2 && filteredLocalItems.value.length === 0);

let searchTimeout: ReturnType<typeof setTimeout> | null = null;
watch(searchQuery, () => {
  if (searchTimeout) clearTimeout(searchTimeout);
  if (isGlobalSearchActive.value) globalData.value = { items: [], page: 0, isLast: false, isLoading: false };
  searchTimeout = setTimeout(() => { if (isGlobalSearchActive.value) loadGlobalData(true); }, 500);
});

const handleScroll = (e: Event) => {
  const el = e.target as HTMLElement;
  if (el.scrollHeight - el.scrollTop <= el.clientHeight + 50) isGlobalSearchActive.value ? loadGlobalData() : loadTabData();
};

const isSelected = (user: any) => selectedUserIds.value.has(getUserId(user));
const getUserId = (user: any): number => Number(user.userId !== undefined ? user.userId : user.id);

const toggleSelection = async (user: any) => {
  const userId = getUserId(user);
  if (mode.value === 'PRIVATE') {
    try { await messengerStore.startPrivateChat(userId); emit('close'); } catch (e) {}
  } else {
    const newSet = new Set(selectedUserIds.value);
    newSet.has(userId) ? newSet.delete(userId) : newSet.add(userId);
    selectedUserIds.value = newSet;
  }
};

const handleSubmit = async () => {
  if (selectedUserIds.value.size === 0) return;

  if (props.purpose === 'ADD_MEMBERS') {
    // Просто отдаем список ID наружу
    emit('add-members', Array.from(selectedUserIds.value));
    emit('close');
  } else {
    if (!groupName.value.trim()) return;
    try {
      await messengerStore.createGroup({
        title: groupName.value, memberIds: Array.from(selectedUserIds.value), avatarUrl: groupAvatarFile.value
      });
      toastStore.success("Группа создана!");
      emit('close');
    } catch (e) {}
  }
};

</script>

<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content chat-creation-modal">
      <header class="modal-header">
        <h3>{{ purpose === 'ADD_MEMBERS' ? 'Добавить участников' : 'Новый чат' }}</h3>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </header>

      <div v-if="purpose !== 'ADD_MEMBERS'" class="mode-tabs">
        <button :class="{ active: mode === 'PRIVATE' }" @click="switchMode('PRIVATE')">👤 Личный</button>
        <button :class="{ active: mode === 'GROUP' }" @click="switchMode('GROUP')">👥 Группа</button>
      </div>

      <Transition name="fade">
        <div v-if="mode === 'GROUP' && purpose !== 'ADD_MEMBERS'" class="group-creation-header">
          <div class="group-avatar-upload" @click="triggerFileUpload">
            <img v-if="groupAvatarPreview" :src="groupAvatarPreview" class="group-avatar-preview" />
            <div v-else class="group-avatar-placeholder"><span>📷</span></div>
          </div>
          <input type="file" ref="fileInput" @change="handleAvatarUpload" accept="image/jpeg, image/png, image/gif" hidden />
          <input v-model="groupName" type="text" placeholder="Название группы..." class="group-name-input-field" />
        </div>
      </Transition>

      <div class="search-input-container">
        <input v-model="searchQuery" type="text" placeholder="Поиск по имени..." class="global-search-input" />
      </div>

      <div class="modal-tabs" v-if="!isGlobalSearchActive">
        <button v-for="tab in tabs" :key="tab.id" class="modal-tab-btn" :class="{ active: activeTab === tab.id }" @click="setTab(tab.id)">
          {{ tab.label }}
        </button>
      </div>

      <div class="friends-picker-list scrollbar" @scroll="handleScroll">
        <template v-if="isGlobalSearchActive">
          <p class="section-label">Глобальный поиск</p>
          <div v-if="globalData.items.filter(isAllowedUser).length === 0 && !globalData.isLoading" class="empty-state">Пользователь не найден</div>
          <div v-for="user in globalData.items.filter(isAllowedUser)" :key="'glob-' + getUserId(user)" class="friend-picker-item" :class="{ 'is-selected': isSelected(user) }" @click="toggleSelection(user)">
            <img :src="user.avatarUrl" class="friend-avatar" />
            <div class="user-info">
              <span class="friend-name">{{ user.username }}</span>
              <span class="user-status">Глобальный поиск</span>
            </div>
            <div v-if="mode === 'GROUP'" class="checkbox"><span v-if="isSelected(user)">✓</span></div>
          </div>
          <div v-if="globalData.isLoading" class="loader-center"><span class="spinner-small"></span></div>
        </template>

        <template v-else>
          <div v-if="filteredLocalItems.length === 0 && !tabData.isLoading" class="empty-state">В этой вкладке пусто</div>
          <div v-for="user in filteredLocalItems" :key="'loc-' + getUserId(user)" class="friend-picker-item" :class="{ 'is-selected': isSelected(user) }" @click="toggleSelection(user)">
            <img :src="user.avatarUrl" class="friend-avatar" />
            <div class="user-info">
              <span class="friend-name">{{ user.username }}</span>
              <span v-if="socialStore.isFriend(getUserId(user))" class="status-badge friend">🤝 Друг</span>
            </div>
            <div v-if="mode === 'GROUP'" class="checkbox"><span v-if="isSelected(user)">✓</span></div>
          </div>
          <div v-if="tabData.isLoading" class="loader-center"><span class="spinner-small"></span></div>
        </template>
      </div>

      <footer v-if="mode === 'GROUP'" class="modal-footer">
        <button class="submit-btn" :disabled="selectedUserIds.size === 0 || (purpose !== 'ADD_MEMBERS' && !groupName.trim())" @click="handleSubmit">
          {{ purpose === 'ADD_MEMBERS' ? 'Добавить' : 'Создать группу' }} ({{ selectedUserIds.size }})
        </button>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
  background: rgba(0,0,0,0.6); backdrop-filter: blur(4px);
  display: flex; justify-content: center; align-items: center; z-index: 9999;
}
.modal-content {
  background: var(--bg-editor-sheet, #2c2c2c); border-radius: 12px;
  border: 1px solid var(--border-subtle, #3d3d3d); width: 400px;
  max-height: 85vh; display: flex; flex-direction: column;
  box-shadow: 0 10px 40px rgba(0,0,0,0.5); overflow: hidden;
}
.modal-header {
  padding: 16px 20px; display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid var(--border-subtle, #3d3d3d);
}
.modal-header h3 { margin: 0; color: #fff; }
.close-btn { background: none; border: none; color: #888; cursor: pointer; font-size: 1.2rem; }
.close-btn:hover { color: #fff; }

.mode-tabs { display: flex; padding: 10px 15px; gap: 10px; }
.mode-tabs button {
  flex: 1; padding: 8px; background: transparent; color: #888;
  border: 1px solid var(--border-subtle, #3d3d3d); border-radius: 6px; cursor: pointer; transition: all 0.2s;
}
.mode-tabs button.active { background: var(--btn-plus, #3498db); color: white; border-color: var(--btn-plus, #3498db); }

/* --- НОВЫЕ СТИЛИ ДЛЯ ШАПКИ СОЗДАНИЯ ГРУППЫ --- */
.group-creation-header {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 15px 5px;
}

.group-avatar-upload {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--bg-main, #1e1e1e);
  border: 2px dashed var(--border-subtle, #3d3d3d);
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.group-avatar-upload:hover {
  border-color: var(--btn-plus, #3498db);
  background: rgba(52, 152, 219, 0.1);
}

.group-avatar-placeholder {
  font-size: 1.2rem;
  opacity: 0.6;
}

.group-avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border: none;
}

.group-name-input-field {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border-subtle, #3d3d3d);
  background: var(--bg-main, #1e1e1e);
  color: white;
  outline: none;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.group-name-input-field:focus {
  border-color: var(--btn-plus, #3498db);
}

.search-input-container { padding: 10px 15px 5px; }
.search-input-container input {
  width: 100%; padding: 10px 12px; border-radius: 6px;
  border: 1px solid var(--border-subtle, #3d3d3d); background: var(--bg-main, #1e1e1e); color: white; outline: none;
}
.modal-tabs { display: flex; gap: 5px; padding: 5px 15px 10px; overflow-x: auto; scrollbar-width: none; }
.modal-tab-btn {
  background: var(--bg-main); border: 1px solid var(--border-subtle); color: var(--text-muted);
  padding: 6px 12px; border-radius: 16px; cursor: pointer; font-size: 0.8rem; white-space: nowrap; transition: all 0.2s;
}
.modal-tab-btn.active { background: var(--btn-plus); color: white; border-color: var(--btn-plus); }

.friends-picker-list { flex: 1; overflow-y: auto; padding: 5px 15px 15px; min-height: 200px; display: flex; flex-direction: column; gap: 8px; }

.friend-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; }
.user-info { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 4px; }
.friend-name { color: #e0e0e0; font-weight: 500; line-height: 1; }
.status-badge { font-size: 0.7rem; padding: 2px 6px; border-radius: 4px; width: fit-content; }
.status-badge.friend { background: rgba(46, 204, 113, 0.2); color: #2ecc71; border: 1px solid rgba(46, 204, 113, 0.4); }

.modal-footer { padding: 15px; border-top: 1px solid var(--border-subtle, #3d3d3d); }
.submit-btn { width: 100%; padding: 12px; border-radius: 8px; background: var(--btn-plus, #3498db); color: white; border: none; font-weight: bold; cursor: pointer; transition: opacity 0.2s; }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.loader-center, .empty-state { text-align: center; color: #888; padding: 20px; font-size: 0.9rem; display: flex; justify-content: center; }
.section-label { font-size: 0.8rem; color: var(--btn-plus); text-transform: uppercase; margin: 5px 0 10px 5px; font-weight: bold; }

.friend-picker-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.friend-picker-item:hover {
  background: rgba(255,255,255,0.05);
}

.friend-picker-item.is-selected {
  background: rgba(52, 152, 219, 0.15) !important;
}

.checkbox {
  width: 22px;
  height: 22px;
  border: 2px solid #555;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 0.8rem;
  transition: all 0.2s ease;
  background: transparent;
}

.friend-picker-item.is-selected .checkbox {
  background: var(--btn-plus, #3498db);
  border-color: var(--btn-plus, #3498db);
}
</style>
