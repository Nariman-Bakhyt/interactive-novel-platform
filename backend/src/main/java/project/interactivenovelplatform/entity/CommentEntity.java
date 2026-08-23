package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment")
@SQLRestriction("is_deleted = false")
@Getter @Setter
@NoArgsConstructor
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUserEntity user;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "timestamp")
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommentEntity parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL)
    private List<CommentEntity> replies = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id")
    private NovelEntity novel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private ChapterEntity chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private ChapterBlockEntity block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_post_id")
    private ChannelPostEntity channelPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forum_topic_id")
    private ForumTopicEntity forumTopic;

    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "metadata")
    private Metadata metadata;

}
