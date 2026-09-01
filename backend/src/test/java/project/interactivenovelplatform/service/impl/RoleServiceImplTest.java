package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.interactivenovelplatform.entity.Role;
import project.interactivenovelplatform.entity.RoleEntity;
import project.interactivenovelplatform.repository.RoleRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleServiceImpl;

    @Test
    @DisplayName("findByName: Успешный поиск существующей роли")
    void findByNamePositive() {
        RoleEntity mockRoleEntity = new RoleEntity();
        mockRoleEntity.setId(1L);
        mockRoleEntity.setName(Role.USER);

        when(roleRepository.findByName(Role.USER)).thenReturn(Optional.of(mockRoleEntity));

        Optional<RoleEntity> result = roleServiceImpl.findByName(Role.USER.name());
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockRoleEntity);
    }

    @Test
    @DisplayName("findByName: Возврат Optional.empty(), когда роль не найдена в БД")
    void findByNameNegative() {

        when(roleRepository.findByName(Role.USER)).thenReturn(Optional.empty());

        Optional<RoleEntity> result = roleServiceImpl.findByName(Role.USER.name()) ;

        assertThat(result).isEmpty();

    }

    @Test
    @DisplayName("findByNameIn: Успешный поиск нескольких ролей")
    void findByNameInPositive() {
        Set<RoleEntity> mockRoleEntities = new HashSet<>();
        mockRoleEntities.add(new RoleEntity(1L,Role.USER));
        mockRoleEntities.add(new RoleEntity(2L,Role.ADMIN));
        when(roleRepository.findByNameIn(Set.of(Role.USER,Role.ADMIN))).thenReturn(mockRoleEntities);

        Set<RoleEntity> result = roleServiceImpl.findByNameIn(Set.of(Role.USER.name(),Role.ADMIN.name()));
        assertThat(result).hasSize(2).isEqualTo(mockRoleEntities);
    }

    @Test
    @DisplayName("findByNameIn: Роли не найдены в БД -> выбрасывает EntityNotFoundException")
    void findByNameInNegative() {

        when(roleRepository.findByNameIn(anySet())).thenReturn(Set.of());

        EntityNotFoundException exception = assertThrowsExactly(EntityNotFoundException.class,
                ()-> roleServiceImpl.findByNameIn(anySet()));

        assertEquals("Роль не найдена", exception.getMessage());
    }

}