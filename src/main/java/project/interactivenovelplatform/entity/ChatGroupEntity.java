package project.interactivenovelplatform.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "chat_group")
@Setter
@Getter
@NoArgsConstructor
public class ChatGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name",nullable = false, length = 255)
    private String name;
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
