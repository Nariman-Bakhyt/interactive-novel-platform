package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_settings")
@Getter@Setter
@NoArgsConstructor
public class UserSettingsEntity {
    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "can_send_message" ,nullable = false)
    private PrivacyLevel canSendMessage = PrivacyLevel.FRIENDS;

    @Enumerated(EnumType.STRING)
    @Column(name = "library_privacy")
    private PrivacyLevel libraryPrivacy = PrivacyLevel.NOBODY;

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_privacy")
    private PrivacyLevel communicationPrivacy = PrivacyLevel.FRIENDS;
}
