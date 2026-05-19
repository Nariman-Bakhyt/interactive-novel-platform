<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref, watch} from 'vue';
import {useSocialStore} from "@/components/social/socialStore.ts";
import {useMessengerStore} from "@/components/chat/messengerStore.ts";
import {useToastStore} from "@/components/toast/toastStore.ts";
import {searchUsers} from "@/api/profileService.ts";
import {getCloseFriends, getFollowers, getFollowing, getFriends} from "@/api/socialService.ts";

const emit = defineEmits(['close', 'add-members']);
const socialStore = useSocialStore();
const messengerStore = useMessengerStore();
const toastStore = useToastStore();

const props = defineProps<{
  purpose?: 'CREATE' | 'ADD_MEMBERS'; 
  excludeIds?: number[]; 
}>();


const mode = ref<'PRIVATE' | 'GROUP'>(props.purpose === 'ADD_MEMBERS' ? 'GROUP' : 'PRIVATE');
const groupName = ref('');
const selectedUserIds = ref<Set<number>>(new Set());

const groupAvatarFile = ref<File | null>(null);
const groupAvatarPreview = ref<string>('');
const fileInput = ref<HTMLInputElement | null>(null);


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
  if (props.purpose === 'ADD_MEMBERS') return; 
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
  return !socialStore.isBlocked(id) && !(props.excludeIds?.includes(id)); 
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
    try { await messengerStore.startPrivateChat(userId); emit('close'); } catch {}
  } else {
    const newSet = new Set(selectedUserIds.value);
    newSet.has(userId) ? newSet.delete(userId) : newSet.add(userId);
    selectedUserIds.value = newSet;
  }
};

const handleSubmit = async () => {
  if (selectedUserIds.value.size === 0) return;

  if (props.purpose === 'ADD_MEMBERS') {
    
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
    } catch {}
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
        <button :class="{ active: mode === 'PRIVATE' }" @click="switchMode('PRIVATE')">
          <span class="icon">👤</span> Личный
        </button>
        <button :class="{ active: mode === 'GROUP' }" @click="switchMode('GROUP')">
          <span class="icon">👥</span> Группа
        </button>
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
          {{ purpose === 'ADD_MEMBERS' ? 'Добавить' : 'Создать группу' }} <span v-if="selectedUserIds.size > 0">({{ selectedUserIds.size }})</span>
        </button>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.6); backdrop-filter: blur(8px);
  display: flex; justify-content: center; align-items: center; z-index: 9999;
}
.modal-content {
  background: var(--bg-dropdown); border-radius: 16px;
  border: 1px solid var(--border-color); width: 100%; max-width: 420px;
  max-height: 85vh; display: flex; flex-direction: column;
  box-shadow: 0 20px 50px rgba(0,0,0,0.5); overflow: hidden;
}
.modal-header {
  padding: 20px 24px; display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid var(--border-color);
}
.modal-header h3 { margin: 0; color: var(--text-header); font-size: 1.25rem; font-weight: 700; }
.close-btn { background: none; border: none; color: var(--text-muted); cursor: pointer; font-size: 1.5rem; transition: color 0.2s; line-height: 1; padding: 4px; border-radius: 4px; }
.close-btn:hover { color: var(--text-header); background: var(--hover-dropdowb);}

.mode-tabs { display: flex; padding: 16px 24px 8px; gap: 12px; }
.mode-tabs button {
  flex: 1; padding: 10px; background: transparent; color: var(--text-muted);
  border: 1px solid var(--border-color); border-radius: 8px; cursor: pointer; transition: all 0.2s;
  font-weight: 500; font-size: 0.95rem; display: flex; align-items: center; justify-content: center; gap: 8px;
}
.mode-tabs button:hover { background: var(--hover-dropdowb); color: var(--text-header); }
.mode-tabs button.active { background: var(--btn-plus); color: white; border-color: var(--btn-plus); font-weight: 600;}


.group-creation-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px 4px;
}

.group-avatar-upload {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--bg-main);
  border: 2px dashed var(--border-color);
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.group-avatar-upload:hover {
  border-color: var(--btn-plus);
  background: rgba(99, 102, 241, 0.1);
}

.group-avatar-placeholder {
  font-size: 1.25rem;
  opacity: 0.5;
}

.group-avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border: none;
}

.group-name-input-field {
  flex: 1;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-main);
  color: var(--text-header);
  outline: none;
  font-size: 1rem;
  transition: border-color 0.2s;
  font-family: inherit;
}

.group-name-input-field:focus {
  border-color: var(--btn-plus);
}

.search-input-container { padding: 12px 24px; }
.search-input-container input {
  width: 100%; padding: 12px 16px; border-radius: 8px;
  border: 1px solid var(--border-color); background: var(--bg-main); color: var(--text-header); outline: none; font-size: 0.95rem; transition: border-color 0.2s;
}
.search-input-container input:focus { border-color: var(--btn-plus); }
.search-input-container input::placeholder { color: var(--input-placeholder); }

.modal-tabs { display: flex; gap: 8px; padding: 4px 24px 12px; overflow-x: auto; scrollbar-width: none; }
.modal-tab-btn {
  background: var(--bg-main); border: 1px solid var(--border-color); color: var(--text-muted);
  padding: 8px 16px; border-radius: 20px; cursor: pointer; font-size: 0.9rem; font-weight: 500; white-space: nowrap; transition: all 0.2s;
}
.modal-tab-btn:hover { background: var(--hover-dropdowb); color: var(--text-header); }
.modal-tab-btn.active { background: var(--btn-plus); color: white; border-color: var(--btn-plus); font-weight: 600;}

.friends-picker-list { flex: 1; overflow-y: auto; padding: 8px 24px 24px; min-height: 240px; display: flex; flex-direction: column; gap: 8px; }

.friend-avatar { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; border: 1px solid var(--border-color); }
.user-info { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 4px; }
.friend-name { color: var(--text-header); font-weight: 600; line-height: 1.2; font-size: 0.95rem; }
.user-status { font-size: 0.8rem; color: var(--text-muted); }
.status-badge { font-size: 0.75rem; padding: 2px 8px; border-radius: 4px; width: fit-content; font-weight: 600; }
.status-badge.friend { background: rgba(16, 185, 129, 0.1); color: #10b981; border: 1px solid rgba(16, 185, 129, 0.2); }

.modal-footer { padding: 20px 24px; border-top: 1px solid var(--border-color); background: var(--bg-dropdown); }
.submit-btn { width: 100%; padding: 14px; border-radius: 8px; background: var(--btn-plus); color: white; border: none; font-weight: 600; font-size: 1rem; cursor: pointer; transition: background 0.2s, transform 0.2s; }
.submit-btn:hover:not(:disabled) { background: var(--btn-plus-hover); transform: translateY(-1px); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.loader-center, .empty-state { text-align: center; color: var(--text-muted); padding: 32px 20px; font-size: 0.95rem; display: flex; justify-content: center; }
.section-label { font-size: 0.8rem; color: var(--btn-plus); text-transform: uppercase; margin: 8px 0 12px 8px; font-weight: 700; letter-spacing: 0.05em; }

.friend-picker-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.friend-picker-item:hover {
  background: var(--hover-dropdowb);
  border-color: var(--border-color);
}

.friend-picker-item.is-selected {
  background: rgba(99, 102, 241, 0.1) !important;
  border-color: rgba(99, 102, 241, 0.2);
}

.checkbox {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border-color);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: transparent;
  font-weight: bold;
  font-size: 0.9rem;
  transition: all 0.2s ease;
  background: var(--bg-main);
}

.friend-picker-item.is-selected .checkbox {
  background: var(--btn-plus);
  border-color: var(--btn-plus);
  color: white;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
