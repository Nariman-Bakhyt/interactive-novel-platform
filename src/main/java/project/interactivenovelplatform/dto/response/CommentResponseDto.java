package project.interactivenovelplatform.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CommentResponseDto {
    private Long id;
    private String content;
    private OffsetDateTime timestamp;

    private Long userId;
    private String username;
    private String userAvatarUrl;

    private Long parentCommentId;
    private Long blockId;
    private Long chapterId;
    private Long novelId;
    private Long forumTopicId;
    private Long channelId;

}
