package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.request.UserRelationRequestDto;
import project.interactivenovelplatform.dto.response.SocialGraphResponseDto;
import project.interactivenovelplatform.dto.response.UserRelationResponseDto;
import project.interactivenovelplatform.entity.*;
import project.interactivenovelplatform.repository.UserBlockRepository;
import project.interactivenovelplatform.repository.UserCloseFriendsRepository;
import project.interactivenovelplatform.repository.UserFollowerRepository;
import project.interactivenovelplatform.repository.UserFriendRepository;
import project.interactivenovelplatform.service.UserService;
import project.interactivenovelplatform.service.UserSocialService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserSocialServiceImpl implements UserSocialService {

    private final UserBlockRepository blockRepository;
    private final UserFollowerRepository followerRepository;
    private final UserFriendRepository friendRepository;
    private final UserCloseFriendsRepository  closeFriendsRepository;
    private final UserService userService;

    private UserRelationResponseDto convertFollowerToDto(UserFollowerEntity relationEntity , AppUserEntity appUserEntity ) {
        return new UserRelationResponseDto(
                relationEntity.getId(),
                appUserEntity.getId(),
                appUserEntity.getUsername(),
                appUserEntity.getAvatarUrl(),
                null,
                relationEntity.getUpdatedAt()
        );
    }

    private UserRelationResponseDto convertFriendToDto(UserFriendEntity friendEntity, AppUserEntity appUserEntity) {
        return new UserRelationResponseDto(
                friendEntity.getId(),
                appUserEntity.getId(),
                appUserEntity.getUsername(),
                appUserEntity.getAvatarUrl(),
                friendEntity.getStatus(),
                friendEntity.getUpdatedAt()
        );
    }


    private UserRelationResponseDto convertCloseFriendToDto(UserCloseFriendsEntity closeFriendsEntity, AppUserEntity appUserEntity) {
        return new UserRelationResponseDto(
                closeFriendsEntity.getId(),
                appUserEntity.getId(),
                appUserEntity.getUsername(),
                appUserEntity.getAvatarUrl(),
                null,
                closeFriendsEntity.getAddedAt()
        );
    }

    private UserRelationResponseDto convertBlockToDto(UserBlockEntity blockEntity , AppUserEntity appUserEntity) {
        return new UserRelationResponseDto(
                blockEntity.getId(),
                appUserEntity.getId(),
                appUserEntity.getUsername(),
                appUserEntity.getAvatarUrl(),
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
        return convertFollowerToDto(relation, receiver);
    }

    @Transactional
    @Override
    public void unfollow(Long currentUserId, UserRelationRequestDto dto) {
        if (dto.getRelationId() != null) {
            UserFollowerEntity relation = followerRepository.findById(dto.getRelationId())
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
        relation.setStatus(RelationStatus.PENDING); // Новая заявка всегда PENDING
        relation.setUpdatedAt(OffsetDateTime.now());

        return convertFriendToDto(friendRepository.save(relation), relation.getReceiver());
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

        return convertFriendToDto(friendRepository.save(relation), relation.getSender());
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
        friendRepository.delete(relation);
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

        return convertCloseFriendToDto(closeFriendsRepository.save(closeFriendRel), friend);
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
        closeFriendsRepository.delete(closeFriendRel);
    }




    @Transactional(readOnly = true)
    @Override
    public PagedModel<UserRelationResponseDto> getIncomingRequests(Long currentUserId, Pageable pageable) {

        var page = friendRepository.findByReceiverIdAndStatus(currentUserId, RelationStatus.PENDING, pageable)
                .map(friend -> convertFriendToDto(friend, friend.getSender()));

        return new PagedModel<>(page);
    }
    @Override
    @Transactional(readOnly = true)
    public PagedModel<UserRelationResponseDto> getOutgoingRequests(Long currentUserId, Pageable pageable) {
        var page = friendRepository.findBySenderIdAndStatus(currentUserId, RelationStatus.PENDING, pageable)
                .map(friend -> convertFriendToDto(friend, friend.getReceiver()));
        return new PagedModel<>(page);
    }

    @Transactional(readOnly = true)
    @Override
    public PagedModel<UserRelationResponseDto> getFollowers(Long currentUserId, Pageable pageable) {
        var page = followerRepository.findByReceiverId(currentUserId, pageable)
                .map(follower -> convertFollowerToDto(follower, follower.getSender()));
        return new PagedModel<>(page);
    }
    @Transactional(readOnly = true)
    @Override
    public PagedModel<UserRelationResponseDto> getFollowingMe(Long currentUserId, Pageable pageable) {
        var page = followerRepository.findBySenderId(currentUserId, pageable)
                .map(follower -> convertFollowerToDto(follower, follower.getReceiver()));
        return new PagedModel<>(page);
    }
    @Transactional(readOnly = true)
    @Override
    public PagedModel<UserRelationResponseDto> getFriends(Long currentUserId, Pageable pageable) {
        var page = friendRepository.findAllFriendsByUserId(currentUserId,RelationStatus.FRIEND,pageable)
                .map(friend ->{
                    AppUserEntity user =  friend.getReceiver().getId().equals(currentUserId) ? friend.getSender() : friend.getReceiver();
                    return convertFriendToDto(friend, user);
                });
        return new PagedModel<>(page);
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
        return convertBlockToDto(blockRepository.save(entity),blocked);
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
    }
    @Transactional(readOnly = true)
    @Override
    public PagedModel<UserRelationResponseDto> getMyBlacklist(Long currentUserId , Pageable pageable) {

        var page = blockRepository.findAllByBlockerId(currentUserId,pageable).map(entity ->convertBlockToDto(entity,entity.getBlocked()));
        return new PagedModel<>(page);
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
}
