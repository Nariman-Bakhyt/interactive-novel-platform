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


export function formatPreciseTime(time: number | Date = new Date()) {
  const d = typeof time === 'number' ? new Date(time) : time;
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  const milliseconds = String(d.getMilliseconds()).padStart(3, '0');

  return `${hours}:${minutes}:${seconds}.${milliseconds}`;
}

export const sentCommentsTimestamps = new Map<string, number>();


export async function createComment(file:File|null , commentRequestDto:CommentRequestDto):Promise<CommentResponseDto>{
  const formData = new FormData();

  if (file) {
    formData.append("files", file);
  }
  const jsonBlob = new Blob([JSON.stringify(commentRequestDto)], {
    type: 'application/json'
  });
  formData.append("comment", jsonBlob);
  
  const startTime = Date.now();
  sentCommentsTimestamps.set(commentRequestDto.content || '', startTime);
  const sendTimeStr = formatPreciseTime(startTime);
  console.log(`%c[HTTP Send] Отправка комментария начата в: ${sendTimeStr}`, "color: #a855f7; font-weight: bold;");
  
  try {
    const response = await apiClient.post<CommentResponseDto>(
      `/comments/send`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        }
      }
    );
    const httpLatency = Date.now() - startTime;
    console.log(`%c[HTTP Latency] Комментарий отправлен. Время HTTP-запроса (Отправка -> Ответ): ${httpLatency}мс`, "color: #3b82f6; font-weight: bold;");
    return response.data;
  } catch (err) {
    throw err;
  }
}

export function useSmartScroll() {
  const route = useRoute();


  const findTextInRange = (container: HTMLElement, textToFind: string): Range[] => {
    const ranges: Range[] = [];
    const searchLower = textToFind.toLowerCase();


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


    const textToFind = route.query.q as string;
    const selfClass = route.query.c as string;
    const prevClass = route.query.p as string;
    const index = Number(route.query.i || 0);

    if (!textToFind) {
      if (typeof CSS !== 'undefined' && CSS.highlights) {
        CSS.highlights.delete("search-results");
      }
      return;
    }

    let attempts = 0;
    const interval = setInterval(() => {
      let ranges: Range[] = [];


      if (selfClass && prevClass) {
        const allPotential = Array.from(document.querySelectorAll(`.${selfClass}`));


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


      if (ranges.length === 0) {
        ranges = findTextInRange(document.body, textToFind);
      }


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


        if (typeof CSS !== 'undefined' && CSS.highlights) {
          const highlight = new Highlight(...ranges);
          CSS.highlights.set("search-results", highlight);


          setTimeout(() => {
            CSS.highlights.delete("search-results");
          }, 5000);
        }
      } else if (attempts >= 15) {

        clearInterval(interval);
      }
      attempts++;
    }, 300);
  };

  return { scrollToTarget };
}
