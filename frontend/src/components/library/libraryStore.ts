import {defineStore} from 'pinia';
import {ref} from 'vue';
import {
  addToLibraryApi,
  getLibraryStatusesApi,
  removeFromLibraryApi
} from '@/api/libraryService.ts';
import type {LibraryStatus} from '@/types/library.ts';
import {PrivacyLevel} from "@/types/user.ts";
import {useToastStore} from "@/components/toast/toastStore.ts";

export const useLibraryStore = defineStore('library', () => {
  const novelStatuses = ref<Record<number, LibraryStatus>>({});
  const isLoaded = ref(false); 
  const toastStore = useToastStore();
  
  const fetchMyStatuses = async (force = false) => {
    if (isLoaded.value && !force) return;

    try {
      const statusesArray = await getLibraryStatusesApi();
      
      statusesArray.forEach(item => {
        novelStatuses.value[item.novelId] = item.status;
      });
      isLoaded.value = true;
    } catch (e) {
      console.error("Не удалось загрузить статусы библиотеки", e);
    }
  };

  const updateStatus = async (novelId: number, status: LibraryStatus, privacyLevel: PrivacyLevel = PrivacyLevel.EVERYONE) => {
    const oldStatus = novelStatuses.value[novelId];
    novelStatuses.value[novelId] = status;
    try {
      await addToLibraryApi({ novelId, status, privacyLevel });
    } catch (e) {
      if (oldStatus) {
        novelStatuses.value[novelId] = oldStatus;
      } else {
        delete novelStatuses.value[novelId];
      }
      toastStore.error('Не удалось сохранить изменения. Проверьте интернет.');
    }
  };

  const removeStatus = async (novelId: number) => {
    const oldStatus = novelStatuses.value[novelId];
    delete novelStatuses.value[novelId];
    try {
      await removeFromLibraryApi(novelId);
    } catch (e) {
      if(oldStatus) {
        novelStatuses.value[novelId] = oldStatus;
      }

    }
  };

  const clearLibrary = () => {
    novelStatuses.value = {};
    isLoaded.value = false;
  }

  return { novelStatuses, fetchMyStatuses, updateStatus, removeStatus, clearLibrary };
});
