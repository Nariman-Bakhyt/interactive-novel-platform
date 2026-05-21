import {Client, type IFrame, type StompSubscription} from '@stomp/stompjs';
import {ref} from "vue";

export const activeSubscriptions = new Map<string, { sub: StompSubscription | null, callbacks: Set<(data: any) => void> }>();
export const isConnected = ref(false);



const getBrokerURL = (): string => {
  if (import.meta.env.PROD) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/ws`;
  }
  return `ws://${import.meta.env.VITE_API_IP}:8080/ws`;
};

const stompClient = new Client({
  brokerURL: getBrokerURL(),
  
  debug: (str) => console.log('STOMP Debug:', str),
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
});
stompClient.beforeConnect = () => {
  const token = localStorage.getItem('jwt_token');

  
  if (!token || token === 'null') {
    console.warn('STOMP: Подключение отменено, jwt_token отсутствует.');
    
    
    return;
  }

  // WebSocket-соединения не отправляют HTTP-заголовки Authorization автоматически. 
  // Передаем токен принудительно через connectHeaders при установлении STOMP-соединения.
  stompClient.connectHeaders = {
    Authorization: `Bearer ${token}`,
  };
};
stompClient.onStompError = (frame: IFrame) => {
  console.error('STOMP Error:', frame.headers['message']);
};

stompClient.onConnect = () => {
  isConnected.value = true;
  console.log('✅ STOMP Connected');

  // Брокер сообщений сбрасывает состояние подписок клиента при разрыве сессии. 
  // Обходим все зарегистрированные коллбеки в реестре activeSubscriptions и восстанавливаем подписки при успешном реконнекте.
  activeSubscriptions.forEach((value, topic) => {
    const newSub = stompClient.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body);
        value.callbacks.forEach(cb => cb(data));
      } catch (e) {
        console.error("STOMP: Ошибка парсинга JSON", e, message.body);
      }
    });
    value.sub = newSub;
    console.log(`🔄 Восстановлена подписка на: ${topic}`);
  });
};

stompClient.onWebSocketClose = () => {
  isConnected.value = false;
  
  
  activeSubscriptions.forEach(val => val.sub = null);
};

export function subscribeToTopic<T>(topic: string, onMessage: (data: T) => void) {
  let subData = activeSubscriptions.get(topic);
  
  if (!subData) {
    subData = { sub: null, callbacks: new Set() };
    activeSubscriptions.set(topic, subData);

    if (!stompClient.active) {
      activateStomp();
    }

    if (stompClient.connected) {
      const sub = stompClient.subscribe(topic, (message) => {
        try {
          const data = JSON.parse(message.body);
          const currentSub = activeSubscriptions.get(topic);
          if (currentSub) {
            currentSub.callbacks.forEach(cb => cb(data));
          }
        } catch (e) {
          console.error("STOMP: Ошибка парсинга JSON", e, message.body);
        }
      });
      subData.sub = sub;
    }
  }
  
  subData.callbacks.add(onMessage as (data: any) => void);
}

export function unsubscribeFromTopic(topic: string, onMessage?: (data: any) => void) {
  const data = activeSubscriptions.get(topic);
  if (data) {
    if (onMessage) {
      data.callbacks.delete(onMessage);
    }
    
    if (!onMessage || data.callbacks.size === 0) {
      if (data.sub) {
        data.sub.unsubscribe();
      }
      activeSubscriptions.delete(topic);
    }
  }
}

export function sendMessage(destination: string, body: any) {
  if (stompClient.connected) {
    stompClient.publish({ destination, body: JSON.stringify(body) });
  }
}

export const activateStomp = () => {
  const token = localStorage.getItem('jwt_token');
  if (token && token !== 'null') {
    stompClient.activate();
  } else {
    console.log('STOMP: Активация невозможна без токена.');
  }
};

export const deactivateStomp = () => {
  stompClient.deactivate();
};
