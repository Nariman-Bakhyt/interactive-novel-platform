import {defineStore} from "pinia";
import {ref} from "vue";

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(localStorage.getItem('theme') !== 'light');

  const applyTheme = () => {
    const theme = isDark.value ? 'dark' : 'light';
    document.body.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  };

  const toggleTheme = () => {
    isDark.value = !isDark.value;
    applyTheme();
  };

  return { isDark, toggleTheme, applyTheme };
});
