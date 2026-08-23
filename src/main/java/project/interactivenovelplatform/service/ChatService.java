package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.ChatSettingsRequestDto;
import project.interactivenovelplatform.dto.request.CreateGroupRequest;
import project.interactivenovelplatform.dto.request.SendMessageRequestDto;
import project.interactivenovelplatform.dto.response.ConversationResponseDto;
import project.interactivenovelplatform.dto.response.MessageResponseDto;

import java.util.List;

public interface ChatService {
    ConversationResponseDto getOrCreatePrivateChat(Long currentUserId, Long targetUserId);
    ConversationResponseDto createGroupChat(Long creatorId, CreateGroupRequest request);
    void sendMessage(Long senderId, SendMessageRequestDto dto, List<MultipartFile> files);
    Page<ConversationResponseDto> getUserChats(Long userId, Pageable pageable);
    Slice<MessageResponseDto> getChatMessages(Long userId, Long conversationId, Pageable pageable);
    void sendTypingStatus(Long userId, Long conversationId);
    void deleteMessage(Long requesterId, Long messageId);
    void deleteChatForUser(Long userId, Long conversationId);
    void leaveGroup(Long userId, Long conversationId);
    void toggleChatSettings(Long userId, ChatSettingsRequestDto settings);
    void kickUser(Long adminId, Long targetUserId, Long conversationId);

    ConversationResponseDto addUserToGroup(Long inviterId, List<Long> targetUserIds, Long conversationId);

    @Transactional
    void markConversationAsRead(Long userId, Long conversationId);
}
