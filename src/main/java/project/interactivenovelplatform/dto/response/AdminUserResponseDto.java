package project.interactivenovelplatform.dto.response;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.interactivenovelplatform.entity.RoleEntity;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponseDto {
    @NotBlank
    private Long id;
    @NotBlank
    @Size(min = 5, max = 50, message = "Логин должен содержать от 4 до 50 символов.")
    private String username;
    @NotBlank(message = "Email не может быть пустым.")
    @Email(message = "Некорректный формат email адреса.")
    private String email;
    @Column(name = "registration_date")
    private ZonedDateTime registrationDate;
    private Set<RoleEntity> role = new HashSet<>();
    @Column(name = "is_deleted",nullable = false)
    private Boolean isDeleted = Boolean.FALSE;
    @Column(name = "is_locked")
    private Boolean isLocked = false;       // Флаг полной блокировки
    @Column(name = "lock_time")
    private ZonedDateTime  lockTime;         // Время, когда блокировка будет снята

}
