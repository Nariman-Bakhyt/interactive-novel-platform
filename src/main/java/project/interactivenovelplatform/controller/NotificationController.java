package project.interactivenovelplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.config.RateLimited;
import project.interactivenovelplatform.dto.response.NotificationResponseDto;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @RateLimited(capacity = 30, minutes = 1)
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDto>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (size > 50) size = 50;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(notificationService.getNotifications(userPrincipal.getId(), pageable));
    }

    @RateLimited(capacity = 20, minutes = 1)
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        notificationService.markAsRead(userPrincipal.getId(), id);
        return ResponseEntity.ok().build();
    }
    
    @RateLimited(capacity = 5, minutes = 1)
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        notificationService.markAllAsRead(userPrincipal.getId());
        return ResponseEntity.ok().build();
    }
}
