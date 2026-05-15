import FingerprintJS from '@fingerprintjs/fingerprintjs';

let cachedId: string | null = null;

// Эта функция запускается ОДИН РАЗ при старте приложения в main.ts
export const initVisitorId = async () => {
  try {
    const fp = await FingerprintJS.load();
    const result = await fp.get();
    cachedId = result.visitorId;
    console.log("Fingerprint initialized:", cachedId);
  } catch (e) {
    console.error("Failed to init fingerprint", e);
    cachedId = "fallback-id-" + Date.now(); // На крайний случай
  }
};

export const getCachedVisitorId = () => cachedId;
