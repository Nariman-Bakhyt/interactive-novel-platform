<script setup lang="ts">
import { ref, onMounted, reactive, watch } from 'vue';
import { useAuthStore } from '@/api/auth';
import { PrivacyLevel } from '@/types/user';

const authStore = useAuthStore();

// Состояние активной вкладки
const activeTab = ref('privacy');

// Локальная копия настроек для редактирования (чтобы не менять глобальный стор до нажатия "Сохранить")
const form = reactive({
  canSendMessage: PrivacyLevel.NOBODY,
  libraryPrivacy: PrivacyLevel.NOBODY,
  // Сюда легко добавить новые поля в будущем:
  // emailNotifications: true,
  // language: 'ru'
});

// Флаги состояния
const isSaving = ref(false);
const saveMessage = ref({ text: '', type: '' });

// Загрузка данных при монтировании
onMounted(async () => {
  await authStore.fetchUserSettings();
  if (authStore.userSettings) {
    syncFormWithStore();
  }
});

// Синхронизация формы с данными из стора
const syncFormWithStore = () => {
  if (authStore.userSettings) {
    form.canSendMessage = authStore.userSettings.canSendMessage;
    form.libraryPrivacy = authStore.userSettings.libraryPrivacy;
  }
};

// Сохранение настроек
const handleSave = async () => {
  isSaving.value = true;
  saveMessage.value = { text: '', type: '' };

  try {
    // Вызываем твой метод из стора
    await authStore.updateUserSettings({
      canSendMessage: form.canSendMessage, // Маппинг на твой RequestDto
      libraryPrivacy: form.libraryPrivacy
    });

    saveMessage.value = { text: 'Настройки успешно сохранены!', type: 'success' };
  } catch (error) {
    saveMessage.value = { text: 'Ошибка при сохранении настроек', type: 'error' };
  } finally {
    isSaving.value = false;
    // Скрываем сообщение через 3 секунды
    setTimeout(() => { saveMessage.value.text = ''; }, 3000);
  }
};
</script>

<template>
  <div class="settings-container">
    <h1 class="settings-title">Настройки</h1>

    <div class="settings-layout">
      <aside class="settings-sidebar">
        <button
          :class="['tab-btn', { active: activeTab === 'account' }]"
          @click="activeTab = 'account'"
        >
          👤 Аккаунт
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'privacy' }]"
          @click="activeTab = 'privacy'"
        >
          🔒 Приватность
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'appearance' }]"
          @click="activeTab = 'appearance'"
        >
          🎨 Внешний вид
        </button>
      </aside>

      <main class="settings-content">
        <section v-if="activeTab === 'privacy'" class="settings-section">
          <h2>Настройки приватности</h2>
          <p class="section-desc">Управляйте тем, кто может видеть вашу активность и взаимодействовать с вами.</p>

          <div class="setting-item">
            <div class="setting-info">
              <label>Кто может писать вам сообщения?</label>
              <span>Ограничьте круг лиц, которые могут отправлять вам личные сообщения.</span>
            </div>
            <select v-model="form.canSendMessage" class="custom-select">
              <option :value="PrivacyLevel.EVERYONE">Все пользователи</option>
              <option :value="PrivacyLevel.FOLLOWERS">Мои подписчики</option>
              <option :value="PrivacyLevel.FRIENDS">Только друзья</option>
              <option :value="PrivacyLevel.BEST_FRIENDS">Близкие друзья</option>
              <option :value="PrivacyLevel.NOBODY">Никто</option>
            </select>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label>Видимость библиотеки</label>
              <span>Кто может просматривать список ваших новелл?</span>
            </div>
            <select v-model="form.libraryPrivacy" class="custom-select">
              <option :value="PrivacyLevel.EVERYONE">Все пользователи</option>
              <option :value="PrivacyLevel.FOLLOWERS">Мои подписчики</option>
              <option :value="PrivacyLevel.FRIENDS">Только друзья</option>
              <option :value="PrivacyLevel.BEST_FRIENDS">Близкие друзья</option>
              <option :value="PrivacyLevel.NOBODY">Только я</option>
            </select>
          </div>
        </section>

        <section v-if="activeTab === 'account'" class="settings-section">
          <h2>Данные аккаунта</h2>
          <p>Раздел в разработке. Здесь будет смена пароля и email.</p>
        </section>

        <section v-if="activeTab === 'appearance'" class="settings-section">
          <h2>Внешний вид</h2>
          <p>Раздел в разработке. Здесь будет кастомизация темы.</p>
        </section>

        <div class="settings-footer">
          <transition name="fade">
            <span v-if="saveMessage.text" :class="['status-msg', saveMessage.type]">
              {{ saveMessage.text }}
            </span>
          </transition>
          <button
            @click="handleSave"
            :disabled="isSaving"
            class="save-btn"
          >
            {{ isSaving ? 'Сохранение...' : 'Сохранить изменения' }}
          </button>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.settings-container {
  max-width: 1000px;
  margin: 80px auto;
  padding: 0 20px;
  color: var(--text-header);
}

.settings-title {
  font-size: 2rem;
  margin-bottom: 30px;
}

.settings-layout {
  display: flex;
  gap: 40px;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  min-height: 500px;
  overflow: hidden;
}

.settings-sidebar {
  width: 250px;
  background: rgba(0, 0, 0, 0.05);
  border-right: 1px solid var(--border-color);
  padding: 20px 0;
}

.tab-btn {
  width: 100%;
  padding: 15px 25px;
  border: none;
  background: none;
  color: var(--text-header);
  text-align: left;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: var(--hover-dropdowb);
}

.tab-btn.active {
  background: var(--btn-plus);
  color: white;
  font-weight: bold;
}

.settings-content {
  flex: 1;
  padding: 30px;
  display: flex;
  flex-direction: column;
}

.settings-section h2 {
  margin-bottom: 10px;
}

.section-desc {
  color: #888;
  margin-bottom: 30px;
  font-size: 0.9rem;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid var(--border-color);
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.setting-info label {
  font-weight: bold;
  font-size: 1.1rem;
}

.setting-info span {
  font-size: 0.85rem;
  color: #777;
}

/* Стили для Select */
.custom-select {
  padding: 10px;
  border-radius: 6px;
  background: var(--bg-header);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  min-width: 180px;
}

/* Стили для Switch */
.switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.switch input { opacity: 0; width: 0; height: 0; }

.slider {
  position: absolute;
  cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 34px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px; width: 18px;
  left: 3px; bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider { background-color: var(--btn-plus); }
input:checked + .slider:before { transform: translateX(26px); }

.settings-footer {
  margin-top: auto;
  padding-top: 30px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
}

.save-btn {
  padding: 12px 30px;
  background: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.status-msg { font-size: 0.9rem; }
.status-msg.success { color: #42b883; }
.status-msg.error { color: #e74c3c; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.5s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
