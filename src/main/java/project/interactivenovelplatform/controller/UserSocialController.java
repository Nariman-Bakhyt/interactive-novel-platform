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
import project.interactivenovelplatform.dto.request.UserRelationRequestDto;
import project.interactivenovelplatform.dto.response.SocialGraphResponseDto;
import project.interactivenovelplatform.dto.response.UserRelationResponseDto;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.UserSocialService;

import java.util.List;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserSocialController {
    private final UserSocialService userSocialService;

    @RateLimited(capacity = 10, minutes = 1)
    @PostMapping("/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserRelationResponseDto> follow(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserRelationRequestDto dto) {
        return ResponseEntity.ok(userSocialService.follow(principal.getId(), dto));
    }
    @RateLimited(capacity = 10, minutes = 1)
    @PostMapping("/unfollow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unfollow(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserRelationRequestDto dto) {
        userSocialService.unfollow(principal.getId(), dto);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
    @RateLimited(capacity = 20, minutes = 1)
    @GetMapping("/followers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<UserRelationResponseDto>> getFollowers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userSocialService.getFollowers(principal.getId(), pageable));
    }
    @RateLimited(capacity = 20, minutes = 1)
    @GetMapping("/following")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<UserRelationResponseDto>> getFollowing(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userSocialService.getFollowingMe(principal.getId(), pageable));
    }


    // ==========================================
    // БЛОК ДРУЗЕЙ (FRIENDS)
    // ==========================================
    @RateLimited(capacity = 5, minutes = 1)
    @PostMapping("/friends/requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserRelationResponseDto> sendFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserRelationRequestDto dto) {
        return ResponseEntity.ok(userSocialService.sendFriendRequest(principal.getId(), dto));
    }
    @RateLimited(capacity = 10, minutes = 1)
    @PostMapping("/friends/requests/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserRelationResponseDto> acceptFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserRelationRequestDto dto) {
        return ResponseEntity.ok(userSocialService.acceptFriendRequest(principal.getId(), dto));
    }
    @RateLimited(capacity = 5, minutes = 1)
    @PostMapping("/friends/requests/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> declineFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserRelationRequestDto dto) {
        userSocialService.declineFriendRequest(principal.getId(), dto);
        return ResponseEntity.noContent().build();
    }
    @RateLimited(capacity = 20, minutes = 1)
    @GetMapping("/friends/requests/incoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<UserRelationResponseDto>> getIncomingRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userSocialService.getIncomingRequests(principal.getId(), pageable));
    }
    @RateLimited(capacity = 20, minutes = 1)
    @GetMapping("/friends/requests/outgoing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<UserRelationResponseDto>> getOutgoingRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userSocialService.getOutgoingRequests(principal.getId(), pageable));
    }
    @RateLimited(capacity = 20, minutes = 1)
    @GetMapping("/friends")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<UserRelationResponseDto>> getFriends(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userSocialService.getFriends(principal.getId(), pageable));
    }

    // ==========================================
    // БЛОК БЛИЗКИХ ДРУЗЕЙ (CLOSE FRIENDS)
    // ==========================================
    @RateLimited(capacity = 20, minutes = 1)
    @GetMapping("/friends/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserRelationResponseDto>> getCloseFriends(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userSocialService.getCloseFriends(principal.getId()));
    }
    @RateLimited(capacity = 10, minutes = 1)
    @PostMapping("/friends/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserRelationResponseDto> addCloseFriend(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserRelationRequestDto dto) {
        return ResponseEntity.ok(userSocialService.addCloseFriend(principal.getId(), dto));
    }
    @RateLimited(capacity = 10, minutes = 1)
    @PostMapping("/friends/close/remove")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeCloseFriend(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserRelationRequestDto dto) {
        userSocialService.removeCloseFriend(principal.getId(), dto);
        return ResponseEntity.noContent().build();
    }
    // ==========================================
    // БЛОК ЧЕРНОГО СПИСКА (BLACKLIST)
    // ==========================================
    @RateLimited(capacity = 5, minutes = 1)
    @PostMapping("/blocks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserRelationResponseDto> blockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserRelationRequestDto dto) {
        return ResponseEntity.ok(userSocialService.blockUser(principal.getId(), dto));
    }
    @RateLimited(capacity = 5, minutes = 1)
    @PostMapping("/blocks/unblock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unblockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserRelationRequestDto dto) {
        userSocialService.unblockUser(principal.getId(), dto);
        return ResponseEntity.noContent().build();
    }
    @RateLimited(capacity = 20, minutes = 1)
    @GetMapping("/blocks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<UserRelationResponseDto>> getMyBlackList(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userSocialService.getMyBlacklist(principal.getId(), pageable));
    }

    @RateLimited(capacity = 5, minutes = 1)
    @GetMapping("/graph")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SocialGraphResponseDto> getSocialGraph(@AuthenticationPrincipal UserPrincipal principal){
        return ResponseEntity.ok(userSocialService.getSocialGraph(principal.getId()));
    }
}
