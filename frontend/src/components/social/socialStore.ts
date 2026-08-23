import {defineStore} from "pinia";
import {ref} from "vue";
import {
  acceptFriendRequest,
  addCloseFriend,
  blockUser,
  declineFriendRequest,
  follow,
  getSocialGraph,
  removeCloseFriend,
  sendFriendRequest,
  unblockUser,
  unfollow
} from "@/api/socialService.ts";
import {SocialEventType, type UserRelationRequestDto} from "@/types/social.ts";
import {useToastStore} from "@/components/toast/toastStore.ts";

const getErrorMessage = (error: any, defaultMsg: string) => {
  return error?.response?.data?.message || error?.message || defaultMsg;
};

export const useSocialStore = defineStore('social', () => {
  
  const followingIds = ref<Map<number, number>>(new Map());
  const followerIds = ref<Map<number, number>>(new Map());
  const friendIds = ref<Map<number, number>>(new Map());
  const closeFriendIds = ref<Map<number, number>>(new Map());
  const blockIds = ref<Map<number, number>>(new Map());
  const incomingRequestIds = ref<Map<number, number>>(new Map());
  const outgoingRequestIds = ref<Map<number, number>>(new Map());

  const toastStore = useToastStore();
  const isLoaded = ref(false);

  const fetchSocialGraph = async (force = false) => {
    if (isLoaded.value && !force) return;
    try {
      const data = await getSocialGraph();
      
      followingIds.value = new Map(Object.entries(data.followingIds).map(([k, v]) => [Number(k), Number(v)]));
      followerIds.value = new Map(Object.entries(data.followerIds).map(([k, v]) => [Number(k), Number(v)]));
      friendIds.value = new Map(Object.entries(data.friendIds).map(([k, v]) => [Number(k), Number(v)]));
      closeFriendIds.value = new Map(Object.entries(data.closeFriendIds).map(([k, v]) => [Number(k), Number(v)]));
      blockIds.value = new Map(Object.entries(data.blockIds).map(([k, v]) => [Number(k), Number(v)]));
      incomingRequestIds.value = new Map(Object.entries(data.incomingRequestIds).map(([k, v]) => [Number(k), Number(v)]));
      outgoingRequestIds.value = new Map(Object.entries(data.outgoingRequestIds).map(([k, v]) => [Number(k), Number(v)]));
      isLoaded.value = true;
    } catch (error) {
      console.error("Ошибка загрузки социального графа", error);
    }
  };

  const isFollowing = (userId: number) => followingIds.value.has(userId);
  const isFollower = (userId: number) => followerIds.value.has(userId);
  const isFriend = (userId: number) => friendIds.value.has(userId);
  const isCloseFriend = (userId: number) => closeFriendIds.value.has(userId);
  const isBlocked = (userId: number) => blockIds.value.has(userId);
  const isIncoming = (userId: number) => incomingRequestIds.value.has(userId);
  const isOutgoing = (userId: number) => outgoingRequestIds.value.has(userId);



  const toggleFollow = async (userId: number) => {
    const relationId = followingIds.value.get(userId) || null;
    const dto: UserRelationRequestDto = { receiverId: userId, relationId: relationId };

    if (isFollowing(userId)) {
      try {
        await unfollow(dto);
        followingIds.value.delete(userId); 
      }catch (error :any) {
        toastStore.error(getErrorMessage(error, 'Не удалось отписаться'));
      }

    } else {
      try {
        const res = await follow(dto);
        followingIds.value.set(userId, res.id);
      }
      catch (error :any) {
        toastStore.error(getErrorMessage(error, 'Не удалось подписаться'));
      }
    }
  };

  const toggleBlock = async (userId: number) => {
    const relationId = blockIds.value.get(userId) || null;
    const dto: UserRelationRequestDto = { receiverId: userId, relationId: relationId };

    const wasBlocked = isBlocked(userId);
    if (wasBlocked) {
      try {
        await unblockUser(dto);
        blockIds.value.delete(userId);
      }
      catch (error :any) {
        toastStore.error(getErrorMessage(error, 'Не удалось разблокировать'));
      }

    } else {
      try {
        const res = await blockUser(dto);
        blockIds.value.set(userId,res.id);
        
        followingIds.value.delete(userId);
        friendIds.value.delete(userId);
        closeFriendIds.value.delete(userId);
        incomingRequestIds.value.delete(userId);
        outgoingRequestIds.value.delete(userId);
      }
      catch (error :any) {
        toastStore.error(getErrorMessage(error, 'Не удалось заблокировать'));
      }
    }
  };

  const toggleCloseFriend = async (userId: number) => {
    const relationId = closeFriendIds.value.get(userId) || null;
    const dto: UserRelationRequestDto = { receiverId: userId, relationId: relationId };
    const wasCloseFriend = isCloseFriend(userId);

    
    if (wasCloseFriend){
      try {
        await removeCloseFriend(dto);
        closeFriendIds.value.delete(userId);
      }catch (error :any) {
        toastStore.error(getErrorMessage(error, 'Не удалось удалить из лучших друзей'));
      }

    }
    else {
      try {
        if (friendIds.value.has(userId)) {
          const res = await addCloseFriend(dto);
          closeFriendIds.value.set(userId,res.id);
        }
        else {
          toastStore.error('нельзя добавить в лучшие друзья пока не стал обычным другом');
        }

      }
      catch (error :any) {
        toastStore.error(getErrorMessage(error, 'Не удалось добавить в лучшие друзья'));
      }
    }
  };

  const sendFriendReq = async (userId: number) => {
    const relationId = friendIds.value.get(userId) || null;
    const dto: UserRelationRequestDto = { receiverId: userId, relationId: relationId };
    try {
      const res = await sendFriendRequest(dto);
      outgoingRequestIds.value.set(userId,res.id);
      toastStore.success('Заявка отправлена');
    } catch (error: any) {
      toastStore.error(getErrorMessage(error, 'Не удалось отправить заявку'));
    }
  };

  const acceptFriendReq = async (userId: number) => {
    const relationId = incomingRequestIds.value.get(userId) || null;
    const dto: UserRelationRequestDto = { receiverId: userId, relationId: relationId };
    try {
      const res = await acceptFriendRequest(dto);
      friendIds.value.set(userId,res.id);
      incomingRequestIds.value.delete(userId);
      toastStore.success('Заявка принята');
    } catch (error: any) {
      toastStore.error(getErrorMessage(error, 'Не удалось принять заявку'));
    }
  };

  const removeFriendOrRequest = async (userId: number) => {
    const relationId = friendIds.value.get(userId) || incomingRequestIds.value.get(userId)
      || outgoingRequestIds.value.get(userId) || null;
    const dto: UserRelationRequestDto = { receiverId: userId, relationId: relationId };
    try {
      await declineFriendRequest(dto);

      friendIds.value.delete(userId);
      incomingRequestIds.value.delete(userId);
      outgoingRequestIds.value.delete(userId);
      closeFriendIds.value.delete(userId); 
    } catch (error: any) {
      toastStore.error(getErrorMessage(error, 'Не удалось удалить пользователя/заявку'));
    }
  };

  const clearGraph= () => {
    followingIds.value.clear();
    followerIds.value.clear();
    friendIds.value.clear();
    closeFriendIds.value.clear();
    blockIds.value.clear();
    incomingRequestIds.value.clear();
    outgoingRequestIds.value.clear();
    isLoaded.value = false;
  }

  const handleSocialEvent = (event: { type: SocialEventType, payload: any }) => {
    const { userId, relationId } = event.payload;

    switch (event.type) {
      
      case SocialEventType.FOLLOW_SUCCESS:
        followingIds.value.set(userId, relationId);
        break;
      case SocialEventType.UNFOLLOW_SUCCESS:
        followingIds.value.delete(userId);
        break;

      
      case SocialEventType.FRIEND_REQUEST_SENT:
        outgoingRequestIds.value.set(userId, relationId);
        break;
      case SocialEventType.FRIEND_REQUEST_RECEIVED:
        incomingRequestIds.value.set(userId, relationId);
        break;
      case SocialEventType.FRIEND_REQUEST_ACCEPTED:
        incomingRequestIds.value.delete(userId);
        outgoingRequestIds.value.delete(userId);
        friendIds.value.set(userId, relationId);
        break;
      case SocialEventType.FRIEND_REQUEST_DECLINED:
        friendIds.value.delete(userId);
        incomingRequestIds.value.delete(userId);
        outgoingRequestIds.value.delete(userId);
        closeFriendIds.value.delete(userId);
        break;

      
      case SocialEventType.CLOSE_FRIEND_ADDED:
        closeFriendIds.value.set(userId, relationId);
        break;
      case SocialEventType.CLOSE_FRIEND_REMOVED:
        closeFriendIds.value.delete(userId);
        break;
      case SocialEventType.USER_BLOCKED:
        blockIds.value.set(userId, relationId);
        
        followingIds.value.delete(userId);
        followerIds.value.delete(userId);
        friendIds.value.delete(userId);
        closeFriendIds.value.delete(userId);
        incomingRequestIds.value.delete(userId);
        outgoingRequestIds.value.delete(userId);
        break;
      case SocialEventType.USER_UNBLOCKED:
        blockIds.value.delete(userId);
        break;
    }
  };

  return {
    followingIds, followerIds, friendIds, closeFriendIds, blockIds, incomingRequestIds, outgoingRequestIds,
    fetchSocialGraph,clearGraph,
    isFollowing, isFollower, isFriend, isCloseFriend, isBlocked,isIncoming ,isOutgoing,isLoaded,
    toggleFollow, toggleBlock, toggleCloseFriend,
    sendFriendReq, acceptFriendReq, removeFriendOrRequest,handleSocialEvent
  };
});
