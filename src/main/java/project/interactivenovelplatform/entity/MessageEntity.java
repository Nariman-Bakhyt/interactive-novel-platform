package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

@Entity
@Table(name = "message")
@SQLRestriction("is_deleted = false")
@Getter @Setter
@NoArgsConstructor
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sender_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_user_sender" , value = ConstraintMode.CONSTRAINT)
    )
    private AppUserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "conversation_id",
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT)
    )
    private ConversationsEntity conversations;

    @Column(name = "content",nullable = false,length = 3000)
    private String content;
    @Column(name = "timestamp")
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @Column(name = "is_deleted")
    private Boolean isDeleted=false;

    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "metadata")
    private MessageMetadata metadata;

}
