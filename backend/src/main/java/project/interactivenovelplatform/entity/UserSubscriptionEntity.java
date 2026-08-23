package project.interactivenovelplatform.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_subscriptions")
@Getter
@Setter
public class UserSubscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private SubscribableEntity target;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private SubscribableType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ChannelRole role = ChannelRole.SUBSCRIBER;

    @Column(name = "is_pinned")
    private boolean isPinned = false;

    @Column(name = "is_muted")
    private boolean isMuted = false;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Column(name = "last_read_at")
    private OffsetDateTime lastReadAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
