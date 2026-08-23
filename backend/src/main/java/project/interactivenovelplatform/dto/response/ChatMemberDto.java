package project.interactivenovelplatform.dto.response;

import lombok.*;
import project.interactivenovelplatform.entity.ConversationMembersRole;

@Builder
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemberDto {
    private Long userId;
    private String username;
    private String avatarUrl;
    private ConversationMembersRole role;
}
