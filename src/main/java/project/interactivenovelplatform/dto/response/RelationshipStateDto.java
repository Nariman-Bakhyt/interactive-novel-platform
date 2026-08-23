package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class RelationshipStateDto {
    private Long userId;
    private boolean isFollowing;
    private boolean isFollower;
    private boolean isFriend;
    private boolean isBestFriend;
    private boolean isBlockedByMe;
    private boolean isBlockedByTarget;

    public boolean anyBlock() {
        return isBlockedByMe || isBlockedByTarget;
    }
}
