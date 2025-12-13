<script setup lang="ts">
import { onMounted, ref } from 'vue';
import {useAuthStore} from "@/stores/auth.ts";
import {useRoute} from "vue-router";

const authStore = useAuthStore();
const isLoading = ref(true);
const showAuthModal = ref(false);
const route = useRoute();

if (route.query.redirect) {
  showAuthModal.value = true;
}

onMounted(async () => {
  try {
    await authStore.fetchUserDetails();
  }
  catch(error){
    console.error("Не удалось загрузить профиль.");
  }finally {
    isLoading.value = false;
  }
})

</script>
<template>
  <div class="profile-container">
    <h2>Мой Профиль</h2>
    <div v-if="isLoading" class="loading">
      Загрузка Данных
    </div>
    <div v-else-if="authStore.userDetails" class="user-info-card">
      <p><strong>ID:</strong>{{authStore.userDetails.id}}</p>
      <p><strong>Логин:</strong>{{authStore.userDetails.username}}</p>
      <p><strong>Email:</strong>{{authStore.userDetails.email}}</p>
    </div>

    <div v-else class="error-message">
      <p> Не удалось загрузить данные. Попробуйте войти снова.</p>
    </div>
  </div>

</template>
<style scoped>

.profile-container {
  max-width: 800px;
  margin: 40px auto;
}
.user-info-card {
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}
.loading, .error-message {
  color: #42b883;
}
</style>
