package project.interactivenovelplatform.dto.response;

import lombok.*;
import project.interactivenovelplatform.entity.Metadata;

import java.time.OffsetDateTime;

@Builder
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponseDto {
    private Long id;
    private Long conversationId;
    private String content;
    private OffsetDateTime timestamp;
    private Metadata metadata;

    
    private Long senderId;
    private String senderUsername;
    private String senderAvatarUrl;
}
