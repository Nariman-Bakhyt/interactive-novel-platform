import {Client, type IFrame, type StompSubscription} from '@stomp/stompjs';
import {ref} from "vue";

export const activeSubscriptions = new Map<string, { sub: StompSubscription | null, callback: (data: any) => void }>();
export const isConnected = ref(false);
// Создаем экземпляр клиента один раз внутри этого файла


const stompClient = new Client({
  brokerURL: `ws://${import.meta.env.VITE_API_IP}:8080/ws`,
  // Убрали статический connectHeaders отсюда
  debug: (str) => console.log('STOMP Debug:', str),
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
});
stompClient.beforeConnect = () => {
  const token = localStorage.getItem('jwt_token');

  // Если токена нет или это строка "null", отменяем попытку
  if (!token || token === 'null') {
    console.warn('STOMP: Подключение отменено, jwt_token отсутствует.');
    // Мы не вызываем deactivate(), чтобы клиент "ждал" следующего вызова activate вручную
    // Но выкидываем ошибку или просто возвращаем, если библиотека позволяет
    return;
  }

  // Динамически подставляем актуальный токен
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

  activeSubscriptions.forEach((value, topic) => {
    const newSub = stompClient.subscribe(topic, (message) => {
      value.callback(JSON.parse(message.body));
    });
    value.sub = newSub;
    console.log(`🔄 Восстановлена подписка на: ${topic}`);
  });
};

stompClient.onWebSocketClose = () => {
  isConnected.value = false;
  // При закрытии сокета обнуляем ссылки на объекты подписок,
  // но оставляем callback-и в Map для восстановления
  activeSubscriptions.forEach(val => val.sub = null);
};

export function subscribeToTopic<T>(topic: string, onMessage: (data: T) => void) {
  if (activeSubscriptions.has(topic)) return;

  activeSubscriptions.set(topic, { sub: null, callback: onMessage });

  // Если мы пытаемся подписаться, а клиент выключен — пробуем включить
  if (!stompClient.active) {
    activateStomp();
  }

  if (stompClient.connected) {
    const sub = stompClient.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body);
        onMessage(data);
      } catch (e) {
        console.error("STOMP: Ошибка парсинга JSON", e, message.body);
      }
    });
    activeSubscriptions.get(topic)!.sub = sub;
  }
}

export function unsubscribeFromTopic(topic: string) {
  const data = activeSubscriptions.get(topic);
  if (data?.sub) {
    data.sub.unsubscribe();
  }
  activeSubscriptions.delete(topic);
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
