package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "forum_topic")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue(SubscribableType.Values.FORUM_TOPIC)
@Getter
@Setter
@NoArgsConstructor
public class ForumTopicEntity extends SubscribableEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUserEntity author;

    @Column(name = "is_locked")
    private boolean isLocked = false;

    @Column(name = "view_count")
    private long viewCount = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

}
