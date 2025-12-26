package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUserEntity implements UserDetails {
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
    private OffsetDateTime registrationDate = OffsetDateTime.now();
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name= "user_id"),
            inverseJoinColumns = @JoinColumn(name= "role_id")
    )
    private Set<RoleEntity> role = new HashSet<>();

    @Column(name = "is_deleted",nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    @Column(name = "failed_attempt_count")
    private Integer failedAttemptCount = 0; // Счетчик неудачных попыток

    @Column(name = "is_locked")
    private Boolean isLocked = false;       // Флаг полной блокировки

    @Column(name = "lock_time")
    private OffsetDateTime  lockTime;         // Время, когда блокировка будет снята

    public AppUserEntity(String username, String passwordHash, String email, Set<RoleEntity> role ) {
        this.role = role;
        this.email = email;
        this.passwordHash = passwordHash;
        this.username = username;
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Если аккаунт не помечен как заблокированный, возвращаем true
        if (!this.isLocked) {
            return true;
        }

        // Если аккаунт заблокирован, проверяем время
        if (this.lockTime != null && this.lockTime.isAfter(OffsetDateTime.now())) {
            // Если время блокировки еще не истекло, аккаунт заблокирован (return false)
            return false;
        }
        else if(this.lockTime == null){
            return false;
        }
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !this.isDeleted; // Используем ваше поле isDeleted
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.role.stream()
                // 1. Получаем имя константы Enum (например, "THE_MAKER" или "ADMIN")
                .map(roleEntity -> roleEntity.getName().name())

                // 2. Добавляем префикс "ROLE_" для Spring Security
                .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
                .collect(Collectors.toList());
    }
}
