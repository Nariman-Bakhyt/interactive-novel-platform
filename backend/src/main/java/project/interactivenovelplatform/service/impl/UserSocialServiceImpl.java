package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.context.ApplicationEventPublisher;
import project.interactivenovelplatform.event.SocialWebsocketEvent;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.SocialEventType;
import project.interactivenovelplatform.dto.request.UserRelationRequestDto;
import project.interactivenovelplatform.dto.response.*;
import project.interactivenovelplatform.entity.*;
import project.interactivenovelplatform.repository.UserBlockRepository;
import project.interactivenovelplatform.repository.UserCloseFriendsRepository;
import project.interactivenovelplatform.repository.UserFollowerRepository;
import project.interactivenovelplatform.repository.UserFriendRepository;
import project.interactivenovelplatform.service.StorageHelper;
import project.interactivenovelplatform.service.StorageService;
import project.interactivenovelplatform.service.UserService;
import project.interactivenovelplatform.service.UserSocialService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserSocialServiceImpl implements UserSocialService {

    private final UserBlockRepository blockRepository;
    private final UserFollowerRepository followerRepository;
    private final UserFriendRepository friendRepository;
    private final UserCloseFriendsRepository  closeFriendsRepository;
    private final UserService userService;
    private final StorageHelper storageHelper;
    private final StorageService storageService;

    private final ApplicationEventPublisher applicationEventPublisher;

    private UserRelationResponseDto convertFollowerToDto(UserFollowerEntity relationEntity , AppUserEntity appUserEntity ) {
        String publicUrl = appUserEntity.getAvatarUrl() != null ? storageService.getPublicUrl(appUserEntity.getAvatarUrl()) : null;
        return new UserRelationResponseDto(
                relationEntity.getId(),
                appUserEntity.getId(),
                appUserEntity.getUsername(),
                storageHelper.getAvatarOrDefault(publicUrl),
                null,
                relationEntity.getUpdatedAt()
        );
    }

    private UserRelationResponseDto convertFriendToDto(UserFriendEntity friendEntity, AppUserEntity appUserEntity) {
        String publicUrl = appUserEntity.getAvatarUrl() != null ? storageService.getPublicUrl(appUserEntity.getAvatarUrl()) : null;
        return new UserRelationResponseDto(
                friendEntity.getId(),
                appUserEntity.getId(),
                appUserEntity.getUsername(),
                storageHelper.getAvatarOrDefault(publicUrl),
                friendEntity.getStatus(),
                friendEntity.getUpdatedAt()
        );
    }


    private UserRelationResponseDto convertCloseFriendToDto(UserCloseFriendsEntity closeFriendsEntity, AppUserEntity appUserEntity) {
        String publicUrl = appUserEntity.getAvatarUrl() != null ? storageService.getPublicUrl(appUserEntity.getAvatarUrl()) : null;
        return new UserRelationResponseDto(
                closeFriendsEntity.getId(),
                appUserEntity.getId(),
                appUserEntity.getUsername(),
                storageHelper.getAvatarOrDefault(publicUrl),
                null,
                closeFriendsEntity.getAddedAt()
        );
    }

    private UserRelationResponseDto convertBlockToDto(UserBlockEntity blockEntity , AppUserEntity appUserEntity) {
        String publicUrl = appUserEntity.getAvatarUrl() != null ? storageService.getPublicUrl(appUserEntity.getAvatarUrl()) : null;
        return new UserRelationResponseDto(
                blockEntity.getId(),
                appUserEntity.getId(),
                appUserEntity.getUsername(),
                storageHelper.getAvatarOrDefault(publicUrl),
                null,
                blockEntity.getCreatedAt()
        );
    }


    @Transactional
    @Override
    public UserRelationResponseDto follow(Long currentUserId, UserRelationRequestDto dto) {
        if(dto.getReceiverId() == null) {
            throw new IllegalArgumentException("Необходимо указать ID пользователя");
        }
        if (currentUserId.equals(dto.getReceiverId())) {
            throw new IllegalArgumentException("Нельзя подписаться на самого себя");
        }
        if (blockRepository.isBlockedEitherWay(currentUserId, dto.getReceiverId())) {
            throw new AccessDeniedException("Невозможно подписаться: один из пользователей находится в черном списке");
        }
        if (followerRepository.existsBySenderIdAndReceiverId(currentUserId, dto.getReceiverId())) {
            throw new IllegalStateException("Вы уже подписаны на этого пользователя");
        }
        var relation = new UserFollowerEntity();
        AppUserEntity receiver = userService.getEntityIsActiveAndIsLockedFalse(dto.getReceiverId());
        relation.setSender(userService.getEntityIsActiveAndIsLockedFalse(currentUserId));
        relation.setReceiver(receiver);
        relation.setUpdatedAt(OffsetDateTime.now());
        followerRepository.save(relation);
        var response = convertFollowerToDto(relation, receiver);

        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + receiver.getId(), new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.FOLLOW_SUCCESS.name(),
                        Map.of("userId", currentUserId ,"relationId" ,relation.getId()))));
        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + currentUserId, new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.FOLLOW_SUCCESS.name(),
                        Map.of("userId", receiver.getId() ,"relationId" ,relation.getId()))));
        return response;
    }

    @Transactional
    @Override
    public void unfollow(Long currentUserId, UserRelationRequestDto dto) {
        UserFollowerEntity relation =  null;
        if (dto.getRelationId() != null) {
            relation = followerRepository.findById(dto.getRelationId())
                    .orElseThrow(() -> new EntityNotFoundException("Подписка не найдена"));
            checkAccess(currentUserId, relation);
            followerRepository.delete(relation);

        } else if (dto.getReceiverId() != null) {
            List<UserFollowerEntity> relations = followerRepository.findAllRelationsBetween(currentUserId, dto.getReceiverId());

            if (!relations.isEmpty()) {
                followerRepository.deleteAll(relations);
            }
        } else {
            throw new IllegalArgumentException("Необходимо указать ID связи или ID пользователя");
        }

        var receiver = relation.getReceiver();
        var sender = relation.getSender();

        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + receiver.getId(), new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.UNFOLLOW_SUCCESS.name(),
                        Map.of("userId", sender.getId() ,"relationId" ,relation.getId()))));

        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + sender.getId(), new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.UNFOLLOW_SUCCESS.name(),
                        Map.of("userId", receiver.getId() ,"relationId" ,relation.getId()))));
    }

    private void checkAccess(Long currentUserId, UserFollowerEntity relation) {
        boolean isSender = relation.getSender().getId().equals(currentUserId);
        boolean isReceiver = relation.getReceiver().getId().equals(currentUserId);
        if (!isSender && !isReceiver) {
            throw new AccessDeniedException("Вы не можете управлять этой подпиской");
        }
    }

    @Transactional
    @Override
    public UserRelationResponseDto sendFriendRequest(Long currentUserId, UserRelationRequestDto dto) {
        if(dto.getReceiverId() == null) {
            throw new IllegalArgumentException("Необходимо указать ID пользователя");
        }
        if (currentUserId.equals(dto.getReceiverId())) {
            throw new IllegalArgumentException("Нельзя отправить заявку самому себе");
        }
        if (blockRepository.isBlockedEitherWay(currentUserId, dto.getReceiverId())) {
            throw new AccessDeniedException("Один из пользователей находится в черном списке");
        }
        if (friendRepository.existsFriendshipBetween(currentUserId, dto.getReceiverId())) {
            throw new IllegalStateException("Связь уже существует (вы друзья или заявка в ожидании)");
        }

        var relation = new UserFriendEntity();
        relation.setSender(userService.getEntityIsActiveAndIsLockedFalse(currentUserId));
        relation.setReceiver(userService.getEntityIsActiveAndIsLockedFalse(dto.getReceiverId()));
        relation.setStatus(RelationStatus.PENDING); 
        relation.setUpdatedAt(OffsetDateTime.now());
        var response = convertFriendToDto(friendRepository.save(relation), relation.getReceiver());

        var receiver = relation.getReceiver();
        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + receiver.getId(), new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.FRIEND_REQUEST_RECEIVED.name(),
                        Map.of("userId", currentUserId ,"relationId" ,response.getId()))));
        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + currentUserId, new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.FRIEND_REQUEST_SENT.name(),
                        Map.of("userId", response.getUserId() ,"relationId" ,response.getId()))));
        return response;
    }
    @Transactional
    @Override
    public UserRelationResponseDto acceptFriendRequest(Long currentUserId, UserRelationRequestDto dto) {
        UserFriendEntity relation;

        if (dto.getRelationId() != null) {
            relation = friendRepository.findUserFriendByIdAndStatusAndReceiverId(dto.getRelationId(),RelationStatus.PENDING,currentUserId)
                    .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена"));
        }
        else if (dto.getReceiverId() != null) {
            relation = friendRepository.findUserFriendBySenderIdAndReceiverIdAndStatus(dto.getReceiverId(),currentUserId,RelationStatus.PENDING )
                    .orElseThrow(() -> new EntityNotFoundException("Входящая заявка от пользователя не найдена"));
        }
        else {
            throw new IllegalArgumentException("Необходимо указать ID связи или ID отправителя заявки");
        }

        relation.setStatus(RelationStatus.FRIEND);
        relation.setUpdatedAt(OffsetDateTime.now());
        var response = convertFriendToDto(friendRepository.save(relation), relation.getReceiver());

        var receiver = relation.getReceiver();
        var sender = relation.getSender();
        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + sender.getId(), new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.FRIEND_REQUEST_ACCEPTED.name(),
                        Map.of("userId", receiver.getId() ,"relationId" ,response.getId()))));
        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + receiver.getId(), new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.FRIEND_REQUEST_ACCEPTED.name(),
                        Map.of("userId", sender.getId() ,"relationId" ,response.getId()))));

        return response;
    }


    @Transactional
    @Override
    public void declineFriendRequest(Long currentUserId, UserRelationRequestDto dto) {
        UserFriendEntity relation;
        if (dto.getRelationId() != null) {
            relation = friendRepository.findById(dto.getRelationId())
                    .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена"));
        } else if (dto.getReceiverId() != null) {
            relation = friendRepository.findRelation(currentUserId, dto.getReceiverId())
                    .orElseThrow(() -> new EntityNotFoundException("Связь между пользователями не найдена"));
        } else {
            throw new IllegalArgumentException("Необходимо указать ID связи или ID пользователя");
        }
        boolean currentUserIsSender = relation.getSender().getId().equals(currentUserId);
        boolean currentUserIsReceiver = relation.getReceiver().getId().equals(currentUserId);
        if(!currentUserIsSender && !currentUserIsReceiver){
            throw new AccessDeniedException("Вы не можете управлять этим отношением");
        }
        var receiver = relation.getReceiver();
        var sender = relation.getSender();
        friendRepository.delete(relation);
        dto.setRelationId(null);
        dto.setReceiverId(receiver.getId());
        removeCloseFriend(currentUserId,dto);

        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + sender.getId(), new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.FRIEND_REQUEST_DECLINED.name(),
                        Map.of("userId", receiver.getId() ,"relationId" ,relation.getId()))));
        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + receiver.getId(), new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.FRIEND_REQUEST_DECLINED.name(),
                        Map.of("userId", sender.getId() ,"relationId" ,relation.getId()))));

    }
    @Transactional
    @Override
    public UserRelationResponseDto addCloseFriend(Long currentUserId, UserRelationRequestDto dto){
        if(dto.getReceiverId() == null) {
            throw new IllegalArgumentException("Необходимо указать ID пользователя");
        }
        Long targetFriendId = dto.getReceiverId();
        if(currentUserId.equals(targetFriendId)){
            throw new IllegalArgumentException("Нельзя добавить самого себя в список близких друзей");
        }
        if(!friendRepository.existsIsFriend(currentUserId, targetFriendId)) {
            throw new IllegalStateException("Пользователь должен быть в списке друзей, чтобы стать близким другом");
        }
        if(closeFriendsRepository.existsByOwnerIdAndFriendId(currentUserId, targetFriendId)){
            throw new IllegalStateException("Пользователь уже находится в списке близких друзей");
        }
        AppUserEntity owner = userService.getEntityIsActiveAndIsLockedFalse(currentUserId);
        AppUserEntity friend = userService.getEntityIsActiveAndIsLockedFalse(targetFriendId);

        UserCloseFriendsEntity closeFriendRel = new UserCloseFriendsEntity();
        closeFriendRel.setOwner(owner);
        closeFriendRel.setFriend(friend);
        closeFriendRel.setAddedAt(OffsetDateTime.now());
        var response = convertCloseFriendToDto(closeFriendsRepository.save(closeFriendRel), friend);
        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + currentUserId, new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.CLOSE_FRIEND_ADDED.name(),
                        Map.of("userId", dto.getReceiverId() ,"relationId" ,closeFriendRel.getId()))));
        return response;
    }

    @Transactional
    @Override
    public void removeCloseFriend(Long currentUserId, UserRelationRequestDto dto) {
        UserCloseFriendsEntity closeFriendRel;

        if (dto.getRelationId() != null) {
            closeFriendRel = closeFriendsRepository.findById(dto.getRelationId())
                    .orElseThrow(() -> new EntityNotFoundException("Запись в списке близких друзей не найдена"));

            if (!closeFriendRel.getOwner().getId().equals(currentUserId)) {
                throw new AccessDeniedException("У вас нет прав на удаление этой связи");
            }
        } else if (dto.getReceiverId() != null) {
            closeFriendRel = closeFriendsRepository.findByOwnerIdAndFriendId(currentUserId, dto.getReceiverId())
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден в списке близких друзей"));
        } else {
            throw new IllegalArgumentException("Необходимо указать ID связи (relationId) или ID пользователя (receiverId)");
        }
        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + currentUserId, new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.CLOSE_FRIEND_REMOVED.name(),
                        Map.of("userId", dto.getReceiverId() ,"relationId" ,closeFriendRel.getId()))));
        closeFriendsRepository.delete(closeFriendRel);
    }




    @Transactional(readOnly = true)
    @Override
    public Slice<UserRelationResponseDto> getIncomingRequests(Long currentUserId, Pageable pageable) {

        return friendRepository.findByReceiverIdAndStatus(currentUserId, RelationStatus.PENDING, pageable)
                .map(friend -> convertFriendToDto(friend, friend.getSender()));
    }
    @Override
    @Transactional(readOnly = true)
    public Slice<UserRelationResponseDto> getOutgoingRequests(Long currentUserId, Pageable pageable) {
        return friendRepository.findBySenderIdAndStatus(currentUserId, RelationStatus.PENDING, pageable)
                .map(friend -> convertFriendToDto(friend, friend.getReceiver()));
    }

    @Transactional(readOnly = true)
    @Override
    public Slice<UserRelationResponseDto> getFollowers(Long currentUserId, Pageable pageable) {
        return followerRepository.findByReceiverId(currentUserId, pageable)
                .map(follower -> convertFollowerToDto(follower, follower.getSender()));
    }
    @Transactional(readOnly = true)
    @Override
    public Slice<UserRelationResponseDto> getFollowingMe(Long currentUserId, Pageable pageable) {
        return followerRepository.findBySenderId(currentUserId, pageable)
                .map(follower -> convertFollowerToDto(follower, follower.getReceiver()));
    }
    @Transactional(readOnly = true)
    @Override
    public Slice<UserRelationResponseDto> getFriends(Long currentUserId, Pageable pageable) {
        return friendRepository.findAllFriendsByUserId(currentUserId,RelationStatus.FRIEND,pageable)
                .map(friend ->{
                    AppUserEntity user =  friend.getReceiver().getId().equals(currentUserId) ? friend.getSender() : friend.getReceiver();
                    return convertFriendToDto(friend, user);
                });
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserRelationResponseDto> getCloseFriends(Long currentUserId){
        return  closeFriendsRepository.findAllByOwnerId(currentUserId).stream()
                .map(friend ->{
                    AppUserEntity user = friend.getFriend();
                    return convertCloseFriendToDto(friend, user);
                }).toList();
    }

    @Transactional
    @Override
    public UserRelationResponseDto blockUser(Long currentUserId, UserRelationRequestDto dto) {
        if (dto.getReceiverId() == null) {
            throw new IllegalArgumentException("Необходимо указать ID пользователя");
        }
        if(dto.getReceiverId().equals(currentUserId)){
            throw new IllegalArgumentException("Нельзя заблокировать самого себя");
        }
        var blocker = userService.getEntityIsActiveAndIsLockedFalse(currentUserId);
        var blocked = userService.getEntityIsActiveAndIsLockedFalse(dto.getReceiverId());
        if(blockRepository.existsUserBlockEntityByBlockerIdAndBlockedId(currentUserId, dto.getReceiverId())) {
            throw new IllegalStateException("Вы уже заблокировали ");
        }
        unfollow(currentUserId, new UserRelationRequestDto(null,dto.getReceiverId()));
        declineFriendRequest(currentUserId, new UserRelationRequestDto(null,dto.getReceiverId()));
        var entity = new UserBlockEntity();
        entity.setBlocker(blocker);
        entity.setBlocked(blocked);
        entity.setCreatedAt(OffsetDateTime.now());
        var response = convertBlockToDto(blockRepository.save(entity),blocked);
        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + currentUserId, new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.USER_BLOCKED.name(),
                        Map.of("userId", dto.getReceiverId() ,"relationId" ,entity.getId()))));
        return response;
    }

    @Transactional
    @Override
    public void unblockUser(Long currentUserId, UserRelationRequestDto dto) {
        if (dto.getRelationId() == null) {
            throw new IllegalArgumentException("Необходимо указать ID связи");
        }
        var blockEntity = blockRepository.findById(dto.getRelationId())
                .orElseThrow(() -> new EntityNotFoundException("Запись о блокировке не найдена"));

        if (!blockEntity.getBlocker().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Вы не можете отменить чужую блокировку");
        }
        blockRepository.delete(blockEntity);

        applicationEventPublisher.publishEvent(new SocialWebsocketEvent(this, "/topic/user." + currentUserId, new WsEventDto<>(WsDomain.SOCIAL,SocialEventType.USER_UNBLOCKED.name(),
                        Map.of("userId", dto.getReceiverId() ,"relationId" ,blockEntity.getId()))));

    }
    @Transactional(readOnly = true)
    @Override
    public Slice<UserRelationResponseDto> getMyBlacklist(Long currentUserId , Pageable pageable) {
        return blockRepository.findAllByBlockerId(currentUserId,pageable).map(entity ->convertBlockToDto(entity,entity.getBlocked()));
    }

    @Override
    public Boolean checkFollower(Long currentUserId , Long followerId){
        return followerRepository.existsBySenderIdAndReceiverId(currentUserId,followerId);

    }
    @Override
    public Boolean checkFriend(Long currentUserId , Long friendId){
        return friendRepository.existsIsFriend(currentUserId,friendId);
    }
    @Override
    public Boolean checkCloseFriends(Long currentUserId , Long friendId){
        return closeFriendsRepository.existsByOwnerIdAndFriendId(friendId,currentUserId);
    }
    @Override
    public Boolean checkBlocked(Long currentUserId, Long friendId){
        return blockRepository.existsUserBlockEntityByBlockerIdAndBlockedId(currentUserId,friendId);
    }
    @Override
    public List<UserBlockRepository.BlockInfo> getAllBlockInfoBetween(Long myId, Set<Long> opponentIds){
        return blockRepository.findAllBlockInfoBetween(myId, opponentIds);
    }

    private Map<Long, Long> getMap(List<Object[]> map) {
        return map.stream()
                .collect(Collectors.toMap(
                        array -> (Long) array[0],
                        array -> (Long) array[1],
                        (existing, replacement) -> existing
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public SocialGraphResponseDto getSocialGraph(Long userId){
        return new SocialGraphResponseDto(
                getMap(followerRepository.findAllFollowerIds(userId)),
                getMap(followerRepository.findAllFollowingIds(userId)),
                getMap(friendRepository.findAllFriendIdsByUserId(userId)),
                getMap(closeFriendsRepository.findCloseFriendIds(userId)),
                getMap(blockRepository.findAllBlockedIds(userId)),
                getMap(friendRepository.findAllIncomingRequests(userId)),
                getMap(friendRepository.findAllOutgoingRequests(userId))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getFollowerIds(Long userId) {
        return followerRepository.findAllFollowerIds(userId).stream()
                .map(obj -> (Long) obj[0])
                .collect(Collectors.toList());
    }
}
