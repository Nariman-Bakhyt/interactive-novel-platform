package project.interactivenovelplatform.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageRequestDto {
    private Long conversationId;
    private String content;
    private String type; 
    private String quoteText;
    private String anchorUrl;
}
