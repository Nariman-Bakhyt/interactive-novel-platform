import type {SocialEventType} from "@/types/social.ts";
import type {WsEventType} from "@/types/chat.ts";

export enum WsDomain{
  CHAT = 'CHAT',
  SOCIAL = 'SOCIAL',
  SYSTEM = 'SYSTEM',
}

type AnyEventType = WsEventType | SocialEventType;

export interface WsEventDto<T> {
  domain: WsDomain;
  type: AnyEventType;
  payload: T;
}
