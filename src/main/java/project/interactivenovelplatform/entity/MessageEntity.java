package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "message")
@Getter
@Setter
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
            name = "receiver_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_user_receiver" , value = ConstraintMode.CONSTRAINT)
    )
    private AppUserEntity receiver;

    @Column(name = "content",nullable = false,columnDefinition = "TEXT")
    private String content;
    @Column(name = "timestamp")
    private ZonedDateTime timestamp = ZonedDateTime.now();
}
