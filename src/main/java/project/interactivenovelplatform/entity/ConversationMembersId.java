package project.interactivenovelplatform.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMembersId implements Serializable {
    private Long conversationId;
    private Long userId;
}
