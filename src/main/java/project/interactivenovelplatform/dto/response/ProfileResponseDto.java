package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponseDto {
    private Long id;
    private String username;
    private String avatarUrl;
    private String email;
    private OffsetDateTime registrationDate;
    private boolean isActive;

    private long novelsCount;
    private long followersCount;
    private long followingCount;
    private long friendsCount;
    private long bestFriendsCount;

    private boolean isMyProfile;
    private boolean isFollowed;
    private boolean isFriend;
    private boolean isBestFriend;
    private boolean isBlockedByMe;
    private boolean isBlockedByTarget;
}
