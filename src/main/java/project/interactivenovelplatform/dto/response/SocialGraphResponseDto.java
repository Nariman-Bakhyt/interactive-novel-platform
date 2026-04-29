package project.interactivenovelplatform.dto.response;

import java.util.Map;

public record SocialGraphResponseDto(
        Map<Long, Long> followerIds,
        Map<Long, Long> followingIds,
        Map<Long, Long> friendIds,
        Map<Long, Long> closeFriendIds,
        Map<Long, Long> blockIds,
        Map<Long, Long> incomingRequestIds,
        Map<Long, Long> outgoingRequestIds
) {}
