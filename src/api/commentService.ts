import apiClient from "./axios.ts";
import type {CommentRequestDto, CommentResponseDto} from "@/types/comment.ts";
import type {SliceModel} from "@/types/PagedModel.ts";
import {nextTick} from "vue";
import {useRoute} from "vue-router";


export async function getComments(params: Partial<CommentRequestDto>, page = 0,
                                  size = 20,
                                  sort: string = 'timestamp,asc'
): Promise<SliceModel<CommentResponseDto>> {
  const response = await apiClient.get(`/comments/public`, {
    params: { ...params, page, size,sort }
  });
  return response.data;
}
export async function deleteComment(commentId: number): Promise<void> {
  await apiClient.delete(`/comments/${commentId}`)

}
export async function createComment(file:File|null , commentRequestDto:CommentRequestDto):Promise<CommentResponseDto>{
  const formData = new FormData();

  if (file) {
    formData.append("files", file);
  }
  const jsonBlob = new Blob([JSON.stringify(commentRequestDto)], {
    type: 'application/json'
  });
  formData.append("comment", jsonBlob);
  const response = await apiClient.post<CommentResponseDto>(
    `/comments/send`,
    formData,
    {
      headers: {
        // Axios сам подставит правильный Boundary, если передать FormData
        'Content-Type': 'multipart/form-data',
      }
    }
  );
  return response.data;
}

export function useSmartScroll() {
  const route = useRoute();

  /**
   * Вспомогательная функция для поиска текста внутри конкретного DOM-узла.
   * Возвращает массив объектов Range, которые можно подсветить.
   */
  const findTextInRange = (container: HTMLElement, textToFind: string): Range[] => {
    const ranges: Range[] = [];
    const searchLower = textToFind.toLowerCase();

    // Создаем навигатор по текстовым узлам, исключая служебные элементы
    const treeWalker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT, {
      acceptNode: (node) => {
        const parent = node.parentElement;
        if (!parent ||
          parent.closest('.global-sidebar') ||
          parent.closest('.quote-floating-btn') ||
          parent.closest('textarea') ||
          parent.closest('input')) {
          return NodeFilter.FILTER_REJECT;
        }
        return NodeFilter.FILTER_ACCEPT;
      }
    });

    let currentNode: Node | null;
    while ((currentNode = treeWalker.nextNode())) {
      const text = currentNode.textContent?.toLowerCase();
      if (!text || !text.includes(searchLower)) continue;

      let startPos = 0;
      while ((startPos = text.indexOf(searchLower, startPos)) !== -1) {
        try {
          const range = document.createRange();
          range.setStart(currentNode, startPos);
          range.setEnd(currentNode, startPos + textToFind.length);
          ranges.push(range);
        } catch (e) {
          console.error("Ошибка создания Range:", e);
        }
        startPos += textToFind.length;
      }
    }
    return ranges;
  };

  const scrollToTarget = async () => {
    await nextTick();

    // Извлекаем параметры из URL
    const textToFind = route.query.q as string; // Текст цитаты
    const selfClass = route.query.c as string;  // Класс самого элемента
    const prevClass = route.query.p as string;  // Класс соседа сверху (контекст)
    const index = Number(route.query.i || 0);   // Индекс в этом контексте

    if (!textToFind) {
      if (typeof CSS !== 'undefined' && CSS.highlights) {
        CSS.highlights.delete("search-results");
      }
      return;
    }

    let attempts = 0;
    const interval = setInterval(() => {
      let ranges: Range[] = [];

      // ЭТАП 1: Прицельный поиск (Свой класс + Класс соседа + Индекс)
      if (selfClass && prevClass) {
        const allPotential = Array.from(document.querySelectorAll(`.${selfClass}`));

        // Фильтруем элементы, проверяя их окружение (соседа или родителя)
        const contextMatches = allPotential.filter(el => {
          const prevEl = el.previousElementSibling;
          return prevEl
            ? prevEl.classList.contains(prevClass)
            : el.parentElement?.classList.contains(prevClass);
        });

        const targetEl = contextMatches[index] as HTMLElement;
        if (targetEl) {
          ranges = findTextInRange(targetEl, textToFind);
        }
      }

      // ЭТАП 2: Если контекст изменился (текст переехал), ищем просто по классу
      if (ranges.length === 0 && selfClass) {
        const elements = document.querySelectorAll(`.${selfClass}`);
        for (const el of elements) {
          const found = findTextInRange(el as HTMLElement, textToFind);
          if (found.length > 0) {
            ranges = found;
            break;
          }
        }
      }

      // ЭТАП 3: Глобальный поиск по всей странице (крайний случай)
      if (ranges.length === 0) {
        ranges = findTextInRange(document.body, textToFind);
      }

      // Если нашли совпадения — скроллим и подсвечиваем
      if (ranges.length > 0) {
        clearInterval(interval);

        const firstRange = ranges[0];
        if (firstRange) {
          const targetElement = firstRange.startContainer instanceof Element
            ? firstRange.startContainer
            : firstRange.startContainer.parentElement;

          if (targetElement) {
            targetElement.scrollIntoView({
              behavior: 'smooth',
              block: 'center'
            });
          }
        }

        // CSS Custom Highlight API
        if (typeof CSS !== 'undefined' && CSS.highlights) {
          const highlight = new Highlight(...ranges);
          CSS.highlights.set("search-results", highlight);

          // Удаляем подсветку через 5 секунд
          setTimeout(() => {
            CSS.highlights.delete("search-results");
          }, 5000);
        }
      } else if (attempts >= 15) {
        // Прекращаем попытки через ~4.5 сек
        clearInterval(interval);
      }
      attempts++;
    }, 300);
  };

  return { scrollToTarget };
}
