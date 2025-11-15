package project.interactivenovelplatform.Entity;

import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sender_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_user_sender" , value = ConstraintMode.CONSTRAINT)
    )
    private AppUser sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "receiver_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_user_receiver" , value = ConstraintMode.CONSTRAINT)
    )
    private AppUser receiver;

    @Column(name = "content",nullable = false,columnDefinition = "TEXT")
    private String content;
    @Column(name = "timestamp")
    private ZonedDateTime timestamp = ZonedDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getSender() {
        return sender;
    }

    public void setSender(AppUser sender) {
        this.sender = sender;
    }

    public AppUser getReceiver() {
        return receiver;
    }

    public void setReceiver(AppUser receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ZonedDateTime getTimetamp() {
        return timestamp;
    }

    public void setTimetamp(ZonedDateTime timetamp) {
        this.timestamp = timetamp;
    }
}
