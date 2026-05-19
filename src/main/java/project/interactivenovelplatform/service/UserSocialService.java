package project.interactivenovelplatform.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import project.interactivenovelplatform.dto.request.UserRelationRequestDto;
import project.interactivenovelplatform.dto.response.SocialGraphResponseDto;
import project.interactivenovelplatform.dto.response.UserRelationResponseDto;
import project.interactivenovelplatform.repository.UserBlockRepository;

import java.util.List;
import java.util.Set;

public interface UserSocialService {
    UserRelationResponseDto follow(Long currentUserId, UserRelationRequestDto dto);
    void unfollow(Long currentUserId, UserRelationRequestDto dto);
    UserRelationResponseDto sendFriendRequest(Long currentUserId, UserRelationRequestDto dto);
    UserRelationResponseDto acceptFriendRequest(Long currentUserId, UserRelationRequestDto dto);
    void declineFriendRequest(Long currentUserId, UserRelationRequestDto dto);
    UserRelationResponseDto addCloseFriend(Long currentUserId, UserRelationRequestDto dto);
    void removeCloseFriend(Long currentUserId, UserRelationRequestDto dto);


    Slice<UserRelationResponseDto> getIncomingRequests(Long currentUserId, Pageable pageable);

    Slice<UserRelationResponseDto> getOutgoingRequests(Long currentUserId, Pageable pageable);

    Slice<UserRelationResponseDto> getFollowers(Long currentUserId, Pageable pageable);
    Slice<UserRelationResponseDto> getFollowingMe(Long currentUserId, Pageable pageable);
    Slice<UserRelationResponseDto> getFriends(Long currentUserId, Pageable pageable);
    List<UserRelationResponseDto> getCloseFriends(Long currentUserId);

    UserRelationResponseDto blockUser(Long currentUserId, UserRelationRequestDto dto);
    void unblockUser(Long currentUserId,UserRelationRequestDto dto );
    Slice<UserRelationResponseDto> getMyBlacklist(Long currentUserId ,Pageable pageable);

    Boolean checkFollower(Long currentUserId , Long followerId);
    Boolean checkFriend(Long currentUserId , Long friendId);
    Boolean checkCloseFriends(Long currentUserId , Long friendId);

    Boolean checkBlocked(Long currentUserId, Long friendId);

    List<UserBlockRepository.BlockInfo> getAllBlockInfoBetween(Long myId, Set<Long> opponentIds);

    SocialGraphResponseDto getSocialGraph(Long userId);
}
