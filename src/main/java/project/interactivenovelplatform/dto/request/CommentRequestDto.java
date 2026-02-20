package project.interactivenovelplatform.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CommentRequestDto {
    private String content;
    private Long parentCommentId;

    private Long blockId;
    private Long chapterId;
    private Long novelId;
    private Long forumTopicId;
    private Long channelId;
}
