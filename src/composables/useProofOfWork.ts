import { ref } from 'vue';
import { sha256 as jsSha256 } from 'js-sha256';

export function useProofOfWork() {
  const isSolving = ref(false);
  const error = ref<Error | null>(null);

  const solveChallenge = (salt: string, difficulty: number): Promise<string> => {
    isSolving.value = true;
    error.value = null;
    const targetPrefix = '0'.repeat(difficulty);
    let nonce = 0;
    const CHUNK_SIZE = 5000;

    return new Promise((resolve, reject) => {
      const computeChunk = () => {
        try {
          for (let i = 0; i < CHUNK_SIZE; i++) {
            const currentNonce = (nonce + i).toString();
            const hash = jsSha256(salt + currentNonce);
            
            if (hash.startsWith(targetPrefix)) {
              isSolving.value = false;
              resolve(currentNonce);
              return;
            }
          }
          nonce += CHUNK_SIZE;
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

  return { isSolving, error, solveChallenge };
}
