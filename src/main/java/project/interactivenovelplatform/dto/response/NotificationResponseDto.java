package project.interactivenovelplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.interactivenovelplatform.entity.NotificationType;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private NotificationType type;
    private Map<String, Object> metadata;
    private Long relatedEntityId;
    
    @JsonProperty("isRead")
    private boolean isRead;
    
    private OffsetDateTime createdAt;
}
