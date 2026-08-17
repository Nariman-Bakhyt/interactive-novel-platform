import type {SocialEventType} from "@/types/social.ts";
import type {WsEventType} from "@/types/chat.ts";
import type {NotificationEventType} from "@/types/notification.ts";

export enum WsDomain{
  CHAT = 'CHAT',
  SOCIAL = 'SOCIAL',
  SYSTEM = 'SYSTEM',
  NOVEL = 'NOVEL',
  NOTIFICATION = 'NOTIFICATION',
}

type AnyEventType = WsEventType | SocialEventType | NotificationEventType;

export interface WsEventDto<T> {
  domain: WsDomain;
  type: AnyEventType;
  payload: T;
}

