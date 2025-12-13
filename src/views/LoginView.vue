<script setup lang="ts">
  import {ref} from 'vue';
  import {useAuthStore} from '../stores/auth';
  import {useRouter} from "vue-router";

  const authStore = useAuthStore();
  const router = useRouter();

  const username=ref('');
  const password=ref('');
  const errorMessage=ref('');

  const handleLogin = async () => {
    try {
      await authStore.login({
        username:username.value,
        password: password.value
      });
      router.push('/');
    }
    catch (err) {
      errorMessage.value = 'Ошибка входа! Проверьте логин и пароль.';
    }

  };
</script>
<template>
  <div class = "login-container">
    <h2 class="black-text">Вход в систему</h2>
    <form @submit.prevent="handleLogin">
      <div class="form-group">
        <label class="black-text">Имя пользователя:</label>
        <input class="black-text" v-model="username" type="text" required placeholder="Введите логин" />
      </div>

      <div class="form-group">
        <label class="black-text">Пароль:</label>
        <input class="black-text" v-model="password" type="password" required placeholder="Введите пароль" />
      </div>
      <p v-if="errorMessage" class="error" >{{errorMessage}} </p>
      <button class="black-text" type="submit">Войти</button>

    </form>
  </div>
</template>
<style scoped>
/* Простые стили для начала */
.black-text{
  color: black;
}
.login-container {
  max-width: 400px;
  margin: 50px auto;
  padding: 20px;
  border: 1px solid black;
  border-radius: 8px;
}
.form-group {
  margin-bottom: 15px;
}
input {
  width: 100%;
  padding: 8px;
  margin-top: 5px;
}
button {
  width: 100%;
  padding: 10px;
  background-color: #42b883; /* Vue Green */
  color: white;
  border: none;
  cursor: pointer;
}
.error {
  color: red;
  font-size: 0.9em;
}
</style>
