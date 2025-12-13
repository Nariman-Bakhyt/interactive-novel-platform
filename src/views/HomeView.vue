<script setup lang="ts">

</script>

<template>
  <div class="hero-section">
    <div class="content">
      <h1>Интерактивные Новеллы</h1>
      <p>Создавай свои миры или погружайся в чужие.</p>

    </div>
  </div>
</template>

<style scoped>
.hero-section {
  height: 100vh;
  width: 100vw;
  /* Красивый фон (замените URL на свою картинку, если есть) */
  background-image: linear-gradient(rgba(0,0,0,0.5), rgba(0,0,0,0.7)), url('https://images.unsplash.com/photo-1519681393784-d120267933ba?q=80&w=2070&auto=format&fit=crop');
  background-size: cover;
  background-position: center;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  text-align: center;
}

.content h1 {
  font-size: 3.5rem;
  margin-bottom: 1rem;
}


</style>
