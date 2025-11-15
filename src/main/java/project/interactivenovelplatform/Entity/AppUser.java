package project.interactivenovelplatform.Entity;

import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public AppUser(String username, String passwordHash, String email,RoleEntity role ) {
        this.role = role;
        this.email = email;
        this.passwordHash = passwordHash;
        this.username = username;
    }

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

    public AppUser() {

    }

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public ZonedDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(ZonedDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
