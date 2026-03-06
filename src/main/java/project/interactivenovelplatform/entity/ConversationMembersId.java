package project.interactivenovelplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMembersId implements Serializable {
    private Long conversationId;
    private Long userId;
}
