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
  margin: 100px auto 60px;
  padding: 0 24px;
  color: var(--text-header);
}

.settings-title {
  font-size: 2.25rem;
  margin-bottom: 32px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.settings-layout {
  display: flex;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  min-height: 500px;
  overflow: hidden;
  box-shadow: 0 4px 12px var(--shadow-color);
}

.settings-sidebar {
  width: 260px;
  background: rgba(0, 0, 0, 0.1);
  border-right: 1px solid var(--border-color);
  padding: 24px 16px;
}

.tab-btn {
  width: 100%;
  padding: 14px 20px;
  border: none;
  background: none;
  color: var(--text-muted);
  text-align: left;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.2s;
  border-radius: 8px;
  margin-bottom: 8px;
  font-weight: 500;
}

.tab-btn:hover {
  background: var(--hover-dropdowb);
  color: var(--text-header);
}

.tab-btn.active {
  background: var(--btn-plus);
  color: white;
  font-weight: 600;
}

.settings-content {
  flex: 1;
  padding: 40px;
  display: flex;
  flex-direction: column;
}

.settings-section h2 {
  margin: 0 0 12px;
  font-size: 1.5rem;
  font-weight: 700;
}

.section-desc {
  color: var(--text-muted);
  margin-bottom: 32px;
  font-size: 0.95rem;
  line-height: 1.5;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 0;
  border-bottom: 1px solid var(--border-color);
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 60%;
}

.setting-info label {
  font-weight: 600;
  font-size: 1.05rem;
}

.setting-info span {
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.4;
}

/* Стили для Select */
.custom-select {
  padding: 10px 36px 10px 16px;
  border-radius: 8px;
  background: var(--bg-header);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  min-width: 220px;
  font-size: 0.95rem;
  appearance: none;
  background-image: url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%23a1a1aa%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E");
  background-repeat: no-repeat;
  background-position: right 16px top 50%;
  background-size: 10px auto;
  cursor: pointer;
  transition: border-color 0.2s;
}

.custom-select:hover, .custom-select:focus {
  border-color: var(--btn-plus);
  outline: none;
}

/* Стили для Switch */
.switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 24px;
}

.switch input { opacity: 0; width: 0; height: 0; }

.slider {
  position: absolute;
  cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: var(--border-color);
  transition: .3s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px; width: 18px;
  left: 3px; bottom: 3px;
  background-color: white;
  transition: .3s;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

input:checked + .slider { background-color: var(--btn-plus); }
input:checked + .slider:before { transform: translateX(24px); }

.settings-footer {
  margin-top: auto;
  padding-top: 32px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 24px;
}

.save-btn {
  padding: 12px 28px;
  background: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 1rem;
  transition: background 0.2s, transform 0.2s;
}
.save-btn:hover:not(:disabled) {
  background: var(--btn-plus-hover);
  transform: translateY(-1px);
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.status-msg { font-size: 0.95rem; font-weight: 500; }
.status-msg.success { color: #10b981; } /* emerald-500 */
.status-msg.error { color: #ef4444; } /* red-500 */

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
