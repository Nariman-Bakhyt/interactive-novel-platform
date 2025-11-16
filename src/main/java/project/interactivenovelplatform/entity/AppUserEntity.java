package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username",nullable = false,unique = true,length = 50)
    private String username;
    @Column(name = "password_hash",nullable = false,length = 255)
    private String passwordHash;
    @Column(name = "email",nullable = false,unique = true,length = 255)
    private String email;
    @Column(name = "registration_date")
    private ZonedDateTime registrationDate=ZonedDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "role_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_role",value = ConstraintMode.CONSTRAINT)
    )
    private RoleEntity role;

    @Column(name = "is_deleted",nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    public AppUserEntity(String username, String passwordHash, String email, RoleEntity role ) {
        this.role = role;
        this.email = email;
        this.passwordHash = passwordHash;
        this.username = username;
    }
}
