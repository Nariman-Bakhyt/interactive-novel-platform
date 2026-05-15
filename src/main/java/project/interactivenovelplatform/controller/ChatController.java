package project.interactivenovelplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    // 1. Получить список всех чатов пользователя
    @GetMapping
    public ResponseEntity<PagedModel<ConversationResponseDto>> getMyChats(
            @AuthenticationPrincipal UserPrincipal user,
            @PageableDefault(size = 20) Pageable pageable ){
        return ResponseEntity.ok(new PagedModel<> (chatService.getUserChats(user.getId(), pageable)));
    }

    // 2. Получить историю сообщений конкретного чата
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<PagedModel<MessageResponseDto>> getChatMessages(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId,
            @PageableDefault(size = 50, sort = "timestamp",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(chatService.getChatMessages(user.getId(), conversationId, pageable)));
    }

    // 3. Начать приватный чат (или получить существующий)
    @PostMapping("/private/{targetUserId}")
    public ResponseEntity<ConversationResponseDto> getOrCreatePrivateChat(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long targetUserId) {
        return ResponseEntity.ok(chatService.getOrCreatePrivateChat(user.getId(), targetUserId));
    }

    // 4. Создать групповый чат
    @PostMapping(value = "/group", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConversationResponseDto> createGroupChat(
            @AuthenticationPrincipal UserPrincipal user,
            @ModelAttribute CreateGroupRequest request) {
        return ResponseEntity.ok(chatService.createGroupChat(user.getId(), request));
    }

    // 5. Отправить сообщение (с поддержкой файлов Multipart)
    @PostMapping(value = "/{conversationId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponseDto> sendMessage(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestPart("dto") SendMessageRequestDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        chatService.sendMessage(user.getId(), dto, files);
        return ResponseEntity.ok().build();
    }

    // 6. Удалить сообщение
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long messageId) {
        chatService.deleteMessage(user.getId(), messageId);
        return ResponseEntity.ok().build();
    }

    // 7. Скрыть/Удалить чат для себя
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteChatForUser(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId) {
        chatService.deleteChatForUser(user.getId(), conversationId);
        return ResponseEntity.ok().build();
    }

    // 8. Настройки чата (Закрепить / Mute)
    @PatchMapping("/conversation/settings")
    public ResponseEntity<Void> toggleSettings(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody ChatSettingsRequestDto settings) {
        chatService.toggleChatSettings(user.getId(), settings);
        return ResponseEntity.ok().build();
    }

    // 9. Выйти из группы
    @PostMapping("/{conversationId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId) {
        chatService.leaveGroup(user.getId(), conversationId);
        return ResponseEntity.ok().build();
    }

    // 10. Добавить пользователя в группу
    @PostMapping("/{conversationId}/members")
    public ResponseEntity<ConversationResponseDto> addUserToGroup(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId,
            @RequestBody List<Long> targetUserIds) {

        return ResponseEntity.ok().body(chatService.addUserToGroup(user.getId(), targetUserIds, conversationId));
    }

    // 11. Исключить пользователя из группы
    @DeleteMapping("/{conversationId}/members/{targetUserId}")
    public ResponseEntity<Void> kickUser(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId,
            @PathVariable Long targetUserId) {
        chatService.kickUser(user.getId(), targetUserId, conversationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{conversationId}/messages/typing")
    public ResponseEntity<Void> sendTypingStatus(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long conversationId){
        chatService.sendTypingStatus(user.getId(), conversationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long conversationId,
                                           @AuthenticationPrincipal UserPrincipal currentUser) {
        chatService.markConversationAsRead(currentUser.getId(), conversationId);
        return ResponseEntity.ok().build();
    }
}
