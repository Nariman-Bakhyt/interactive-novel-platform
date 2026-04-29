package project.interactivenovelplatform.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import project.interactivenovelplatform.dto.request.UserRelationRequestDto;
import project.interactivenovelplatform.dto.response.SocialGraphResponseDto;
import project.interactivenovelplatform.dto.response.UserRelationResponseDto;

import java.util.List;

public interface UserSocialService {
    UserRelationResponseDto follow(Long currentUserId, UserRelationRequestDto dto);
    void unfollow(Long currentUserId, UserRelationRequestDto dto);
    UserRelationResponseDto sendFriendRequest(Long currentUserId, UserRelationRequestDto dto);
    UserRelationResponseDto acceptFriendRequest(Long currentUserId, UserRelationRequestDto dto);
    void declineFriendRequest(Long currentUserId, UserRelationRequestDto dto);
    UserRelationResponseDto addCloseFriend(Long currentUserId, UserRelationRequestDto dto);
    void removeCloseFriend(Long currentUserId, UserRelationRequestDto dto);


    PagedModel<UserRelationResponseDto> getIncomingRequests(Long currentUserId, Pageable pageable);

    PagedModel<UserRelationResponseDto> getOutgoingRequests(Long currentUserId, Pageable pageable);

    PagedModel<UserRelationResponseDto> getFollowers(Long currentUserId, Pageable pageable);
    PagedModel<UserRelationResponseDto> getFollowingMe(Long currentUserId, Pageable pageable);
    PagedModel<UserRelationResponseDto> getFriends(Long currentUserId, Pageable pageable);
    List<UserRelationResponseDto> getCloseFriends(Long currentUserId);

    UserRelationResponseDto blockUser(Long currentUserId, UserRelationRequestDto dto);
    void unblockUser(Long currentUserId,UserRelationRequestDto dto );
    PagedModel<UserRelationResponseDto> getMyBlacklist(Long currentUserId ,Pageable pageable);

    Boolean checkFollower(Long currentUserId , Long followerId);
    Boolean checkFriend(Long currentUserId , Long friendId);
    Boolean checkCloseFriends(Long currentUserId , Long friendId);

    SocialGraphResponseDto getSocialGraph(Long userId);
}
