package project.interactivenovelplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.config.RateLimited;
import project.interactivenovelplatform.dto.request.UserLibraryRequestDto;
import project.interactivenovelplatform.dto.response.UserLibraryResponseDto;
import project.interactivenovelplatform.dto.response.UserLibraryStatusDto;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.UserLibraryService;

import java.util.List;

@RestController
@RequestMapping("/api/userlibrary")
@RequiredArgsConstructor
public class UserLibraryController {
    private final UserLibraryService userLibraryService;

    @RateLimited(capacity = 20, minutes = 1)
    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<UserLibraryResponseDto>> getUserLibrary(@PathVariable Long userId ,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long currentUserId = principal.getId();
        return ResponseEntity.ok().body(userLibraryService.getUserLibrary(currentUserId,userId,pageable));
    }
    @RateLimited(capacity = 15, minutes = 1)
    @PutMapping()
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserLibraryResponseDto> updateUserLibrary(@RequestBody UserLibraryRequestDto dto, @AuthenticationPrincipal UserPrincipal principal) {
        Long currentUserId = principal.getId();
        return ResponseEntity.ok().body(userLibraryService.addOrUpdateLibraryEntry(currentUserId,dto));
    }
    @RateLimited(capacity = 50, minutes = 5)
    @DeleteMapping("/{novelId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteUserLibrary(@PathVariable Long novelId, @AuthenticationPrincipal UserPrincipal principal) {
        Long currentUserId = principal.getId();
        userLibraryService.removeFromLibrary(currentUserId,novelId);
        return ResponseEntity.ok().build();
    }
    @RateLimited(capacity = 30, minutes = 1)
    @GetMapping("/statuses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserLibraryStatusDto>> getMyLibraryStatuses(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userLibraryService.getUserLibraryStatuses(principal.getId()));
    }
}
