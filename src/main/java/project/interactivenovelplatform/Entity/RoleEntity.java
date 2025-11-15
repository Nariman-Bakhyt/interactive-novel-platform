package project.interactivenovelplatform.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class RoleEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public RoleEntity(Role name) {
        this.name = name;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "name",nullable = false,unique = true)
    private Role name;

    public RoleEntity() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Role getName() {
        return name;
    }

    public void setName(Role name) {
        this.name = name;
    }
}
