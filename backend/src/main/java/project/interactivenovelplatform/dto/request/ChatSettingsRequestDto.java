package project.interactivenovelplatform.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatSettingsRequestDto {
    private Long conversationId;
    private Boolean isPinned;
    private Boolean isMuted;
}
