package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CommentRequestDto {
    private String content;
    private Long parentCommentId;
    private String type;
    private String quoteText;
    private String anchorUrl;

    private Long blockId;
    private Long chapterId;
    private Long novelId;
    private Long forumTopicId;
    private Long channelId;

    @AssertTrue(message = "Должен быть указан ровно один целевой идентификатор (blockId, chapterId, novelId, forumTopicId, channelId)")
    public boolean isValidTarget() {
        int count = 0;
        if (blockId != null) count++;
        if (chapterId != null) count++;
        if (novelId != null) count++;
        if (forumTopicId != null) count++;
        if (channelId != null) count++;
        return count == 1;
    }
}
