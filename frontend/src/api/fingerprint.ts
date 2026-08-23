import FingerprintJS from '@fingerprintjs/fingerprintjs';

let cachedId: string | null = null;


// Кэшируем visitorId при инициализации приложения. Это позволяет Axios-интерцепторам считывать его мгновенно и синхронно, избегая асинхронных задержек на каждый HTTP-запрос.
export const initVisitorId = async () => {
  try {
    const fp = await FingerprintJS.load();
    const result = await fp.get();
    cachedId = result.visitorId;
    console.log("Fingerprint initialized:", cachedId);
  } catch (e) {
    console.error("Failed to init fingerprint", e);
    // Резервный fallback обеспечивает отказоустойчивость (fault tolerance) при блокировках скрипта браузером / блокировщиками рекламы.
    cachedId = "fallback-id-" + Date.now(); 
  }
};

export const getCachedVisitorId = () => cachedId;
