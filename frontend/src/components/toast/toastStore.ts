import {defineStore} from 'pinia';
import {ref} from 'vue';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastMessage {
  id: number;
  text: string;
  type: ToastType;
}

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<ToastMessage[]>([]);
  let nextId = 0;

  const showToast = (text: string, type: ToastType = 'info', duration = 3000) => {
    const id = nextId++;

    
    toasts.value.push({ id, text, type });

    
    setTimeout(() => {
      removeToast(id);
    }, duration);
  };

  const removeToast = (id: number) => {
    toasts.value = toasts.value.filter(t => t.id !== id);
  };

  const success = (text: string) => showToast(text, 'success');
  const error = (text: string) => showToast(text, 'error');
  const info = (text: string) => showToast(text, 'info');

  return { toasts, showToast, success, error, info, removeToast };
});
