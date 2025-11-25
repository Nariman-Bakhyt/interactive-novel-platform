package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.dto.request.RoleRequestDto;
import project.interactivenovelplatform.dto.request.UserNameRequestDto;
import project.interactivenovelplatform.dto.response.AdminUserResponseDto;
import project.interactivenovelplatform.service.AdminUserService;

import java.util.Set;


@RestController
@AllArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping("/admin/users/{id}")
    public AdminUserResponseDto findById(@PathVariable Long id){
        return adminUserService.findById(id);
    }

    @GetMapping("/admin/users/all")
    public Page<AdminUserResponseDto> findAllUsers(
            @PageableDefault(size = 10) Pageable pageable
    ){
        return adminUserService.findAll(pageable);
    }

    @PatchMapping("/admin/users/addrole/{id}")
    public AdminUserResponseDto addRoleToUser(
            @PathVariable Long id,
            @RequestBody @Valid RoleRequestDto role
    ){
        return adminUserService.addRoleToUser(id, role);
    }

    @PostMapping("/admin/users/setrole/{id}")
    public AdminUserResponseDto setRoleToUser(
            @PathVariable Long id,
            @RequestBody @Valid Set<RoleRequestDto> role
    ){
        return adminUserService.setRolesToUser(id, role);
    }

    @GetMapping("/admin/user")
    public AdminUserResponseDto findUserByName(@RequestBody @Valid UserNameRequestDto name){
        return  adminUserService.findByUsername(name);
    }

}
