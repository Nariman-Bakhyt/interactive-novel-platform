package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "conversation_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMembersEntity {
    @EmbeddedId
    private ConversationMembersId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id")
    private ConversationsEntity conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private AppUserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role" , length = 20)
    private ConversationMembersRole role = ConversationMembersRole.MEMBER;
    @Column(name = "joined_at")
    private OffsetDateTime joinedAt = OffsetDateTime.now();


}
