package project.interactivenovelplatform.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_blocks")
@Getter@Setter
@NoArgsConstructor
public class UserBlockEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id" , nullable = false)
    private AppUserEntity blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id" , nullable = false)
    private AppUserEntity blocked;

    @Column(name = "created_at")
    private OffsetDateTime createdAt =  OffsetDateTime.now();
}
