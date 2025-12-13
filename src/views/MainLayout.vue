<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue';
import { RouterView } from 'vue-router';
</script>

<template>
  <div>
    <AppHeader />
    <main class="content-wrapper">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.content-wrapper {
  padding-top: 30px;
}
</style>
