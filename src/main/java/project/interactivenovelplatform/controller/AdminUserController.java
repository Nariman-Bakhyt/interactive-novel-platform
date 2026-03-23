package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.dto.request.RoleRequestDto;
import project.interactivenovelplatform.dto.request.UserNameRequestDto;
import project.interactivenovelplatform.dto.response.AdminUserResponseDto;
import project.interactivenovelplatform.service.AdminUserService;

import java.util.Set;


@RestController
@RequestMapping("/api/admin")
@AllArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping("/users/{id}")
    @PreAuthorize("@rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN)")
    public AdminUserResponseDto findById(@PathVariable Long id){
        return adminUserService.findById(id);
    }

    @GetMapping("/users/all")
    @PreAuthorize("@rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN)")
    public Page<AdminUserResponseDto> findAllUsers(
            @PageableDefault(size = 10) Pageable pageable
    ){
        return adminUserService.findAll(pageable);
    }

    @PatchMapping("/users/addrole/{id}")
    @PreAuthorize("@rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN)")
    public AdminUserResponseDto addRoleToUser(
            @PathVariable Long id,
            @RequestBody @Valid RoleRequestDto role
    ){
        return adminUserService.addRoleToUser(id, role);
    }

    @PostMapping("/users/setrole/{id}")
    @PreAuthorize("@rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN)")
    public AdminUserResponseDto setRoleToUser(
            @PathVariable Long id,
            @RequestBody @Valid Set<RoleRequestDto> role
    ){
        return adminUserService.setRolesToUser(id, role);
    }

    @GetMapping("/user")
    @PreAuthorize("@rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN)")
    public AdminUserResponseDto findUserByName(@RequestBody @Valid UserNameRequestDto name){
        return  adminUserService.findByUsername(name);
    }

}
