package project.interactivenovelplatform.dto.response;

import lombok.*;
import project.interactivenovelplatform.entity.ConversationsType;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConversationResponseDto {
    private Long id ;
    private ConversationsType type ;
    private String title ;
    private String avatarUrl ;
    private OffsetDateTime lastMessageAt ;
    private String lastMessagePreview ;
    private List<ChatMemberDto> members ;
    private boolean blocked;
    private OffsetDateTime lastReadAt;
    private boolean isMuted;
    private boolean isPinned;
}
