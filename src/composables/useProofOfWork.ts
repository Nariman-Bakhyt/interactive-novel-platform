import { ref } from 'vue';
import { getCachedVisitorId } from '@/api/fingerprint';
import { sha256 as jsSha256 } from 'js-sha256';

export function useProofOfWork() {
  const isSolving = ref(false);
  const error = ref<Error | null>(null);

  // Обертка для быстрого SHA-256 через Web Crypto API с фоллбеком на js-sha256
  const sha256 = async (message: string) => {
    if (window.crypto && window.crypto.subtle) {
      try {
        const msgBuffer = new TextEncoder().encode(message);
        const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
      } catch (e) {
        console.warn("Web Crypto API failed, falling back to pure JS", e);
      }
    }
    // Fallback для небезопасных контекстов (например, http:// на мобильном телефоне)
    return jsSha256(message);
  };

  const solveChallenge = async (salt: string, difficulty: number): Promise<string> => {
    isSolving.value = true;
    error.value = null;
    const targetPrefix = '0'.repeat(difficulty);
    let nonce = 0;
    
    // Разбиваем работу на чанки, чтобы не фризить UI
    const CHUNK_SIZE = 5000;

    return new Promise((resolve, reject) => {
      const computeChunk = async () => {
        try {
          for (let i = 0; i < CHUNK_SIZE; i++) {
            const currentNonce = (nonce + i).toString();
            const hash = await sha256(salt + currentNonce);
            
            if (hash.startsWith(targetPrefix)) {
              isSolving.value = false;
              resolve(currentNonce);
              return;
            }
          }
          nonce += CHUNK_SIZE;
          // Отдаем управление браузеру на отрисовку UI и продолжаем работу
          setTimeout(computeChunk, 0);
        } catch (err: any) {
          isSolving.value = false;
          error.value = err;
          reject(err);
        }
      };

      computeChunk();
    });
  };

  const obtainGuestId = async () => {
    try {
      const vId = getCachedVisitorId();
      // 1. Получаем задачу (используем fetch, чтобы не сработал axios interceptor)
      const challengeRes = await fetch('/api/auth/public/challenge', {
        headers: {
          'X-Visitor-Id': vId || ''
        }
      });
      const challengeData = await challengeRes.json();
      
      if (challengeRes.status === 429) {
          throw new Error(challengeData.message || "Слишком много попыток. Подождите 10 минут.");
      }
      if (!challengeRes.ok) throw new Error("Rate limit exceeded or server error");

      // 2. Решаем задачу в фоне (без фриза UI)
      const nonce = await solveChallenge(challengeData.salt, challengeData.difficulty);

      // 3. Проверяем решение и получаем cookie / guest_id
      const verifyRes = await fetch('/api/auth/public/verify-challenge', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ salt: challengeData.salt, nonce })
      });

      const verifyData = await verifyRes.json();
      if (verifyRes.ok) {
         return verifyData;
      } else {
         throw new Error(verifyData.message || "Failed to verify challenge");
      }
    } catch (err: any) {
      error.value = err;
      throw err;
    } finally {
      isSolving.value = false;
    }
  };

  return { isSolving, error, obtainGuestId, solveChallenge };
}
