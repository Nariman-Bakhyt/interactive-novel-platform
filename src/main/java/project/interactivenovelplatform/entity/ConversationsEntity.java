package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ConversationsType type = ConversationsType.PRIVATE;

    @Column(name = "title",length = 255)
    private String title;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "conversation")
    private List<MessageEntity> messages;

    @OneToMany(mappedBy = "conversation",cascade = CascadeType.ALL , orphanRemoval = true)
    @Builder.Default
    private List<ConversationMembersEntity> members = new ArrayList<>();

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt = OffsetDateTime.now();

    @Column(name = "last_message_preview", length = 255)
    private String lastMessagePreview;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;
}
