package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.config.RateLimited;
import project.interactivenovelplatform.dto.request.ChatSettingsRequestDto;
import project.interactivenovelplatform.dto.request.CreateGroupRequest;
import project.interactivenovelplatform.dto.request.SendMessageRequestDto;
import project.interactivenovelplatform.dto.response.ConversationResponseDto;
import project.interactivenovelplatform.dto.response.MessageResponseDto;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.ChatService;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    
    @RateLimited(capacity = 100, minutes = 1)
    @GetMapping
    public ResponseEntity<PagedModel<ConversationResponseDto>> getMyChats(
            @AuthenticationPrincipal UserPrincipal user,
            @PageableDefault(size = 20) Pageable pageable ){
        if (pageable.getPageSize() > 50) {
            pageable = PageRequest.of(pageable.getPageNumber(), 50, pageable.getSort());
        }
        return ResponseEntity.ok(new PagedModel<> (chatService.getUserChats(user.getId(), pageable)));
    }

    
    @RateLimited(capacity = 100, minutes = 1)
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<Slice<MessageResponseDto>> getChatMessages(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId,
            @PageableDefault(size = 50, sort = "timestamp",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        return ResponseEntity.ok(chatService.getChatMessages(user.getId(), conversationId, pageable));
    }

    
    @RateLimited(capacity = 10, minutes = 1)
    @PostMapping("/private/{targetUserId}")
    public ResponseEntity<ConversationResponseDto> getOrCreatePrivateChat(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long targetUserId) {
        return ResponseEntity.ok(chatService.getOrCreatePrivateChat(user.getId(), targetUserId));
    }

    
    @RateLimited(capacity = 10, minutes = 1)
    @PostMapping(value = "/group", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConversationResponseDto> createGroupChat(
            @AuthenticationPrincipal UserPrincipal user,
            @ModelAttribute CreateGroupRequest request) {
        return ResponseEntity.ok(chatService.createGroupChat(user.getId(), request));
    }

    
    @RateLimited(capacity = 30, minutes = 1)
    @PostMapping(value = "/{conversationId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponseDto> sendMessage(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestPart("dto") SendMessageRequestDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        chatService.sendMessage(user.getId(), dto, files);
        return ResponseEntity.ok().build();
    }

    
    @RateLimited(capacity = 10, minutes = 1)
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long messageId) {
        chatService.deleteMessage(user.getId(), messageId);
        return ResponseEntity.ok().build();
    }

    
    @RateLimited(capacity = 10, minutes = 1)
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteChatForUser(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId) {
        chatService.deleteChatForUser(user.getId(), conversationId);
        return ResponseEntity.ok().build();
    }

    
    @RateLimited(capacity = 10, minutes = 1)
    @PatchMapping("/conversation/settings")
    public ResponseEntity<Void> toggleSettings(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody ChatSettingsRequestDto settings) {
        chatService.toggleChatSettings(user.getId(), settings);
        return ResponseEntity.ok().build();
    }

    
    @RateLimited(capacity = 10, minutes = 1)
    @PostMapping("/{conversationId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId) {
        chatService.leaveGroup(user.getId(), conversationId);
        return ResponseEntity.ok().build();
    }

    
    @RateLimited(capacity = 10, minutes = 1)
    @PostMapping("/{conversationId}/members")
    public ResponseEntity<ConversationResponseDto> addUserToGroup(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable @Valid Long conversationId,
            @RequestBody List<Long> targetUserIds) {

        return ResponseEntity.ok().body(chatService.addUserToGroup(user.getId(), targetUserIds, conversationId));
    }

    
    @RateLimited(capacity = 10, minutes = 1)
    @DeleteMapping("/{conversationId}/members/{targetUserId}")
    public ResponseEntity<Void> kickUser(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId,
            @PathVariable Long targetUserId) {
        chatService.kickUser(user.getId(), targetUserId, conversationId);
        return ResponseEntity.ok().build();
    }

    @RateLimited(capacity = 60, minutes = 1)
    @PostMapping("/{conversationId}/messages/typing")
    public ResponseEntity<Void> sendTypingStatus(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId){
        chatService.sendTypingStatus(user.getId(), conversationId);
        return ResponseEntity.ok().build();
    }

    @RateLimited(capacity = 60, minutes = 1)
    @PostMapping("/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long conversationId,
                                           @AuthenticationPrincipal UserPrincipal currentUser) {
        chatService.markConversationAsRead(currentUser.getId(), conversationId);
        return ResponseEntity.ok().build();
    }
}