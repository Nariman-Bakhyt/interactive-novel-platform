package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "verification_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_verification_type",
                        columnNames = {"user_id", "type"}
                )
        }
)
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class VerificationTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column( nullable = false , length = 64 )
    private String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUserEntity user;

    @Column( name = "expiry_date")
    private OffsetDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private VerificationTokenType type;

    @Column(name = "pending_value",length = 555)
    private String pendingValue;
}
