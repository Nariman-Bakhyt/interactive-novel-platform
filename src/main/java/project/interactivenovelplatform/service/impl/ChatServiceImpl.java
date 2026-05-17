package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.FinalizedMessageData;
import project.interactivenovelplatform.dto.request.ChatSettingsRequestDto;
import project.interactivenovelplatform.dto.request.CreateGroupRequest;
import project.interactivenovelplatform.dto.request.SendMessageRequestDto;
import project.interactivenovelplatform.dto.response.*;
import project.interactivenovelplatform.entity.*;
import project.interactivenovelplatform.error.GlobalExceptionHandler;
import project.interactivenovelplatform.repository.ConversationMemberRepository;
import project.interactivenovelplatform.repository.ConversationRepository;
import project.interactivenovelplatform.repository.MessageRepository;
import project.interactivenovelplatform.repository.UserBlockRepository;
import project.interactivenovelplatform.security.UrlValidator;
import project.interactivenovelplatform.service.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ConversationRepository conversationRepo;
    private final ConversationMemberRepository memberRepo;
    private final MessageRepository messageRepo;
    private final UserService userService;
    private final UserSocialService userSocialService;

    private final StorageService storageService;
    private final TransactionTemplate transactionTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final StorageHelper storageHelper;
    private final static Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    private MessageResponseDto convertToMessageResponse(MessageEntity entity) {
        Metadata metadata = entity.getMetadata();
        // Если в сообщении есть картинки, превращаем их пути в полные URL
        if (metadata != null && metadata.getImages() != null) {
            List<String> fullUrls = metadata.getImages().stream()
                    .map(imagePath -> {
                        if (imagePath.startsWith("chat/")) {
                            return storageService.getPresignedUrl(imagePath);
                        }
                        return storageService.getPublicUrl(imagePath);
                    })
                    .toList();
            Metadata responseMetadata = new Metadata();
            responseMetadata.setType(metadata.getType());
            responseMetadata.setImages(fullUrls);
            metadata = responseMetadata;
        }

        String senderAvatar = entity.getSender().getAvatarUrl() != null 
                ? storageService.getPublicUrl(entity.getSender().getAvatarUrl()) : null;

        return new MessageResponseDto(
                entity.getId(),
                entity.getConversation().getId(),
                entity.getContent(),
                entity.getTimestamp(),
                metadata,
                entity.getSender().getId(),
                entity.getSender().getUsername(),
                storageHelper.getAvatarOrDefault(senderAvatar)
        );
    }

    private ConversationResponseDto convertToConversationResponse(ConversationsEntity entity, boolean isBlocked,ConversationMembersEntity conversationMembers ,Long currentUserId) {

        String finalTitle = entity.getTitle();
        String finalAvatar = entity.getAvatarUrl();

        if (entity.getType() == ConversationsType.PRIVATE) {
            var opponentMember = entity.getMembers().stream()
                    .filter(m -> !m.getUser().getId().equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            if (opponentMember != null) {
                AppUserEntity opponent = opponentMember.getUser();

                if (finalTitle == null || finalTitle.isBlank()) {
                    finalTitle = opponent.getUsername();
                }
                if (finalAvatar == null || finalAvatar.isBlank()) {
                    String opponentAvatar = opponent.getAvatarUrl() != null ? storageService.getPublicUrl(opponent.getAvatarUrl()) : null;
                    finalAvatar = storageHelper.getAvatarOrDefault(opponentAvatar);
                }
            } else if (finalAvatar != null) {
                finalAvatar = storageService.getPublicUrl(finalAvatar);
            }
        }
        if (finalAvatar == null) finalAvatar = storageHelper.getAvatarOrDefault(null);

        List<ChatMemberDto> members = entity.getMembers().stream()
                .map(m -> new ChatMemberDto(
                        m.getUser().getId(),
                        m.getUser().getUsername(),
                        storageHelper.getAvatarOrDefault(m.getUser().getAvatarUrl() != null ? storageService.getPublicUrl(m.getUser().getAvatarUrl()) : null),
                        m.getRole()
                ))
                .collect(Collectors.toList());

        return new ConversationResponseDto(
                entity.getId(),
                entity.getType(),
                finalTitle,
                finalAvatar,
                entity.getLastMessageAt(),
                entity.getLastMessagePreview(),
                members,
                isBlocked,
                conversationMembers.getLastReadAt(),
                conversationMembers.isMuted(),
                conversationMembers.isPinned()
        );
    }

    private ConversationResponseDto convertToConversationResponsePrivate(ConversationsEntity entity, boolean isBlocked,ConversationMembersEntity conversationMembers) {
        List<ChatMemberDto> members = entity.getMembers().stream()
                .map(m -> new ChatMemberDto(
                        m.getUser().getId(),
                        m.getUser().getUsername(),
                        m.getUser().getAvatarUrl() != null ? storageService.getPublicUrl(m.getUser().getAvatarUrl()) : null,
                        m.getRole()
                ))
                .collect(Collectors.toList());

        return new ConversationResponseDto(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getAvatarUrl(),
                entity.getLastMessageAt(),
                entity.getLastMessagePreview(),
                members,
                isBlocked,
                conversationMembers.getLastReadAt(),
                conversationMembers.isMuted(),
                conversationMembers.isPinned()
        );
    }

    private ConversationMembersEntity addMember(ConversationsEntity chat, AppUserEntity user, ConversationMembersRole role) {
        ConversationMembersId id = new ConversationMembersId(chat.getId(), user.getId());

        // Проверяем, нет ли уже такого участника в списке чата
        return chat.getMembers().stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElseGet(() -> {
                    ConversationMembersEntity member = new ConversationMembersEntity();
                    member.setId(id);
                    member.setConversation(chat);
                    member.setUser(user);
                    member.setRole(role);

                    chat.getMembers().add(member);
                    return member;
                });
    }

    private void validateChatAccess(Long currentUserId, Long targetUserId) {
        RelationshipStateDto relation = userService.getRelationshipState(currentUserId, targetUserId);
        if (relation.isBlockedByTarget()) {
            throw new AccessDeniedException("Вы находитесь в черном списке этого пользователя");
        }
        if (relation.isBlockedByMe()) {
            throw new AccessDeniedException("Пользователь находится в вашем черном списке. Сначала разблокируйте его");
        }

        var settings = userService.getUserSettings(targetUserId);
        PrivacyLevel canSendMessage = settings.getCanSendMessage();

        boolean isFriend = relation.isFriend();
        boolean isBestFriend = relation.isBestFriend();
        boolean isFollower = relation.isFollowing();

        boolean accessGranted = switch (canSendMessage) {
            case EVERYONE -> true;
            case FOLLOWERS -> isFollower || isFriend || isBestFriend;
            case FRIENDS -> isFriend || isBestFriend;
            case BEST_FRIENDS -> isBestFriend;
            case NOBODY -> false;
        };

        if (!accessGranted) {
            String message = switch (canSendMessage) {
                case FOLLOWERS -> "Пользователь разрешил сообщения только своим подписчикам";
                case FRIENDS -> "Пользователь разрешил сообщения только друзьям";
                case BEST_FRIENDS -> "Пользователь разрешил сообщения только близким друзьям";
                case NOBODY -> "Пользователь запретил отправку личных сообщений всем";
                default -> "Доступ ограничен настройками приватности";
            };
            throw new AccessDeniedException(message);
        }
    }

    private List<Long> filterAvailableUsers(Long inviterId, List<Long> targetUserIds) {
        Map<Long, RelationshipStateDto> relations = userService.getRelationshipStates(inviterId, targetUserIds);
        Map<Long, UserSettingsResponseDto> allSettings = userService.getUserSettingsMap(targetUserIds);

        List<Long> allowedIds = new ArrayList<>();

        for (Long targetId : targetUserIds) {
            RelationshipStateDto relation = relations.get(targetId);
            UserSettingsResponseDto settings = allSettings.get(targetId);

            // 1. Проверка на существование данных (на случай битых ID)
            if (relation == null || settings == null) {
                log.warn("Данные для пользователя {} не найдены, пропускаем", targetId);
                continue;
            }

            // 2. Проверка черных списков
            if (relation.isBlockedByTarget() || relation.isBlockedByMe()) {
                log.info("Пользователь {} пропущен: блокировка в отношениях", targetId);
                continue;
            }

            // 3. Проверка приватности
            PrivacyLevel privacy = settings.getCanSendMessage();
            boolean isFriend = relation.isFriend();
            boolean isBestFriend = relation.isBestFriend();
            boolean isFollower = relation.isFollowing();

            boolean accessGranted = switch (privacy) {
                case EVERYONE -> true;
                case FOLLOWERS -> isFollower || isFriend || isBestFriend;
                case FRIENDS -> isFriend || isBestFriend;
                case BEST_FRIENDS -> isBestFriend;
                case NOBODY -> false;
            };

            if (accessGranted) {
                allowedIds.add(targetId);
            } else {
                log.info("Пользователь {} пропущен: настройки приватности ({})", targetId, privacy);
            }
        }
        return allowedIds;
    }
    @Transactional
    @Override
    public ConversationResponseDto getOrCreatePrivateChat(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) throw new IllegalArgumentException("Нельзя с самим собой");
        boolean isBlocked = userSocialService.checkBlocked(targetUserId, currentUserId);
        validateChatAccess(currentUserId, targetUserId);

        ConversationsEntity chat = conversationRepo.findPrivateConversation(currentUserId, targetUserId)
            .orElseGet(() -> {
                ConversationsEntity newChat = new ConversationsEntity();
                newChat.setType(ConversationsType.PRIVATE);
                newChat.setCreatedAt(OffsetDateTime.now());
                newChat.setLastMessageAt(OffsetDateTime.now());
                newChat.setMembers(new ArrayList<>()); // инициализируем список

                newChat = conversationRepo.save(newChat);

                AppUserEntity currentUser = userService.getEntityIsActiveAndIsLockedFalse(currentUserId);
                AppUserEntity targetUser = userService.getEntityIsActiveAndIsLockedFalse(targetUserId);

                addMember(newChat, currentUser, ConversationMembersRole.MEMBER);
                addMember(newChat, targetUser, ConversationMembersRole.MEMBER);
                return newChat;
            });

        ConversationMembersEntity conversationMember = chat.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Участник не найден в чате " + chat.getId()));

        AppUserEntity opponent = chat.getMembers().stream()
                .map(ConversationMembersEntity::getUser)
                .filter(u -> !u.getId().equals(currentUserId))
                .findFirst()
                .orElse(null);


        return convertToConversationResponse(chat, isBlocked,conversationMember,currentUserId);
    }

    // ==========================================
    // 2. СОЗДАНИЕ ГРУППЫ
    // ==========================================
    @Override
    public ConversationResponseDto createGroupChat(Long creatorId, CreateGroupRequest request) {

        OffsetDateTime now = OffsetDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String folderPath = String.format("avatars/group/%s", datePath);
        String newAvatarUrl = null;
        var file = request.getAvatarUrl();
        if (file != null && !file.isEmpty()) {
            try {
                String actualMimeType = storageService.verifyRealImageType(file);
                String secureExtension = MimeTypes.getDefaultMimeTypes()
                        .forName(actualMimeType).getExtension();

                String filename = UUID.randomUUID().toString().substring(0, 8) + System.currentTimeMillis() + secureExtension;
                newAvatarUrl = storageService.uploadFile(file, folderPath, filename);
            } catch (Exception e) {
                log.warn("Не удалось загрузить обложку для группы {}: {}", creatorId, e.getMessage());
            }
        }
        final String finalNewAvatarUrl = newAvatarUrl;
        try {
            ConversationResponseDto responseDto = transactionTemplate.execute(_ ->{
                AppUserEntity creator = userService.getEntityIsActiveAndIsLockedFalse(creatorId);

                ConversationsEntity group = new ConversationsEntity();
                group.setType(ConversationsType.GROUP);
                group.setTitle(request.getTitle());
                group.setCreatedAt(now);
                group.setAvatarUrl(finalNewAvatarUrl);
                group = conversationRepo.save(group);

                // Создатель становится админом
                var me = addMember(group, creator, ConversationMembersRole.ADMIN);
                // Добавляем остальных
                for (Long memberId : request.getMemberIds()) {
                    try {
                        validateChatAccess(creatorId, memberId);
                        AppUserEntity member = userService.getEntityIsActiveAndIsLockedFalse(memberId);
                        addMember(group, member, ConversationMembersRole.MEMBER);
                    } catch (AccessDeniedException e) {
                        // Просто игнорируем этого пользователя и идем дальше
                    }
                }

                return convertToConversationResponse(group,false,me ,creatorId);
            });
            return responseDto;
        }
        catch (Exception e) {
            if (finalNewAvatarUrl != null) {
                storageService.deleteFile(finalNewAvatarUrl);
            }
            throw new RuntimeException("Ошибка при создании группы: " + e.getMessage());
        }


    }

    // ==========================================
    // 3. ОТПРАВКА СООБЩЕНИЯ
    // ==========================================
    @Override
    public void sendMessage(Long senderId, SendMessageRequestDto dto, List<MultipartFile> files) {
        // Шаг 1: Сохраняем скелет в транзакции
        OffsetDateTime now = OffsetDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Metadata metadata = createChatMetadata(files, datePath, dto, senderId);

        transactionTemplate.execute(_ -> {
            ConversationMembersEntity membership = memberRepo.findByConversationIdAndUserId(dto.getConversationId(), senderId)
                    .orElseThrow(() -> new RuntimeException("Нет доступа к чату"));
            membership.setLastReadAt(now);
            memberRepo.save(membership);
            ConversationsEntity conversation =membership.getConversation();
            if(conversation.getType() == ConversationsType.PRIVATE) {
                validateChatAccess(senderId, getOpponentId(conversation,senderId));
            }
            MessageEntity message = new MessageEntity();
            message.setSender(membership.getUser());
            message.setConversation(membership.getConversation());
            message.setContent(dto.getContent() != null ? dto.getContent() : "");
            message.setTimestamp(now);
            message.setIsDeleted(false);
            message.setMetadata(metadata);
            messageRepo.saveAndFlush(message);
            // 2. Апдейт самого чата

            conversation.setLastMessageAt(now);
            String content = dto.getContent();
            // Превью сообщения (если есть текст - берем его, если фото - пишем "Фотография")
            String preview = content != null && !content.isBlank() ?
                    (content.length() > 50 ? content.substring(0, 47) + "..." : content)
                    : ("IMAGE".equals(metadata.getType()) ? "📷 Фотография" : "Отправил вложение");
            conversation.setLastMessagePreview(preview);

            // 3. Воскрешаем чат у тех, кто его удалил (скрыл)
            memberRepo.restoreAllMembersInConversation(conversation.getId());
            // 2. Получаем ID всех участников для рассылки (тоже одним запросом)
            List<Long> memberIds = memberRepo.findAllMemberIdsByConversationId(conversation.getId());

            conversationRepo.save(conversation);

            MessageResponseDto response = convertToMessageResponse(message);

            messagingTemplate.convertAndSend(
                    "/topic/chat." + dto.getConversationId(),
                    new WsEventDto<>(WsDomain.CHAT,WsEventType.NEW_MESSAGE.name(), response)
            );

            for (Long memberId : memberIds) {
                messagingTemplate.convertAndSend(
                        "/topic/user." + memberId ,
                        new WsEventDto<>(WsDomain.CHAT,WsEventType.CHAT_UPDATED.name(), response)
                );
            }
            return new FinalizedMessageData(message,memberIds);
        });

    }


    // ==========================================
    // 2. ФОРМИРОВАНИЕ METADATA
    // ==========================================
    private Metadata createChatMetadata(List<MultipartFile> files, String datePath, SendMessageRequestDto dto, Long userId) {
        Metadata metadata = new Metadata();
        try {
            if ("IMAGE".equals(dto.getType())) {
                if(files == null || files.isEmpty()) throw new IllegalArgumentException("Нет фото");
                if (files.size() > 10) throw new RuntimeException("Слишком много файлов!");

                List<String> imageUrls = new ArrayList<>();
                for (MultipartFile file : files) {
                    String actualMimeType = storageService.verifyRealImageType(file);
                    String secureExtension = MimeTypes.getDefaultMimeTypes().forName(actualMimeType).getExtension();
                    // Изменили папку на chat
                    String folderPath = String.format("chat/%d/%s", dto.getConversationId(), datePath);
                    String finalFileName = UUID.randomUUID().toString().substring(0, 8) + secureExtension;
                    imageUrls.add(storageService.uploadFile(file, folderPath, finalFileName));
                }
                metadata.setType("IMAGE");
                metadata.setImages(imageUrls);
            } else if("QUOTE".equals(dto.getType())) {
                if (dto.getQuoteText() == null || dto.getQuoteText().isBlank() ||
                        dto.getAnchorUrl() == null || dto.getAnchorUrl().isBlank()) {
                    throw new BadRequestException("Для цитаты необходим текст и ссылка на источник");
                }
                if (!UrlValidator.isTrusted(dto.getAnchorUrl())) {
                    throw new BadRequestException("Ссылка ведет на недоверенный ресурс");
                }
                // 2. Формируем чистую ссылку с Query-параметром ?q=
                String rawText = dto.getQuoteText();
                // Кодируем текст (пробелы станут %20, а не плюсики)
                String encodedText = URLEncoder.encode(rawText, StandardCharsets.UTF_8).replace("+", "%20");

                String finalUrl = dto.getAnchorUrl();

                // Если в anchorUrl уже есть параметры (содержит ?), добавляем через &, если нет — через ?
                String separator = finalUrl.contains("?") ? "&" : "?";
                finalUrl += separator + "q=" + encodedText;
                metadata.setType("QUOTE");
                metadata.setQuoteText(dto.getQuoteText());
                metadata.setAnchorUrl(finalUrl);
            } else {
                metadata.setType("PLAIN");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка обработки файлов", e);
        }
        return metadata;
    }


    // ==========================================
    // 4. ПОЛУЧЕНИЕ СПИСКА ЧАТОВ (Левое меню)
    // ==========================================
    @Transactional(readOnly = true)
    @Override
    public Page<ConversationResponseDto> getUserChats(Long userId, Pageable pageable) {

        Pageable simplePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<ConversationMembersEntity> memberships = memberRepo.findActiveChatsForUser(userId, simplePageable);

        Set<Long> opponentIds = memberships.stream()
                .filter(m -> m.getConversation().getType() == ConversationsType.PRIVATE)
                .map(m -> getOpponentId(m.getConversation(), userId))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Получаем только необходимые ID (память скажет "спасибо")
        List<UserBlockRepository.BlockInfo> blockInfos = userSocialService.getAllBlockInfoBetween(userId, opponentIds);

        Set<Long> blockers = new HashSet<>();
        Set<Long> blockedByMe = new HashSet<>();

        for (var info : blockInfos) {
            if (info.getBlockedId().equals(userId)) {
                blockers.add(info.getBlockerId());
            } else {
                blockedByMe.add(info.getBlockedId());
            }
        }

        return memberships.map(m -> {
            ConversationsEntity chat = m.getConversation();
            boolean amIBlocked = false;
            boolean isPartnerBlockedByMe = false;
            boolean isBlocked = false;

            if (chat.getType() == ConversationsType.PRIVATE) {
                Long opponentId = getOpponentId(chat, userId);
                if (opponentId != null) { // Безопасная проверка
                    amIBlocked = blockers.contains(opponentId);
                    isPartnerBlockedByMe = blockedByMe.contains(opponentId);
                }
            }
            isBlocked = amIBlocked || isPartnerBlockedByMe ;
            return convertToConversationResponse(chat, isBlocked, m,userId);
        });
    }
    private Long getOpponentId(ConversationsEntity chat, Long userId) {
        return chat.getMembers().stream()
                .map(member -> member.getUser().getId()) // Берем ID всех участников
                .filter(id -> !id.equals(userId))        // Убираем твой ID
                .findFirst()                             // Оставляем ID собеседника
                .orElse(null);                           // Если чат пустой (мало ли), вернет null
    }

    // ==========================================
    // 5. ПОЛУЧЕНИЕ ИСТОРИИ СООБЩЕНИЙ ЧАТА
    // ==========================================
    @Transactional(readOnly = true)
    @Override
    public Page<MessageResponseDto> getChatMessages(Long userId, Long conversationId, Pageable pageable) {
        // Проверяем, есть ли у юзера доступ к этому чату
        var conversationMember = memberRepo.findByConversationIdAndUserId(conversationId, userId)
                .filter(m -> !m.isDeleted()
                ) // Если он скрыл чат, он не должен видеть сообщения, пока не напишет сам
                .orElseThrow(() -> new RuntimeException("Нет доступа к чату"));
        OffsetDateTime clearedAt = conversationMember.getClearedAt();
        // Получаем сообщения (с сортировкой от новых к старым)
        return messageRepo.findMessages(conversationId,clearedAt,pageable)
                .map(this::convertToMessageResponse);
    }
    @Override
    public void sendTypingStatus(Long userId, Long conversationId) {
        AppUserEntity user = userService.getEntityIsActiveAndIsLockedFalse(userId);

        Map<String, Object> payload = Map.of(
                "userId", userId,
                "username", user.getUsername()
        );

        messagingTemplate.convertAndSend(
                "/topic/chat." + conversationId,
                new WsEventDto<>(WsDomain.CHAT,WsEventType.USER_TYPING.name(), payload)
        );
    }

    // ==========================================
    // 6. УДАЛЕНИЕ СООБЩЕНИЯ (Мягкое)
    // ==========================================
    @Transactional
    @Override
    public void deleteMessage(Long requesterId, Long messageId) {
        MessageEntity message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

        // Проверка: удалять может только автор
        if (!message.getSender().getId().equals(requesterId)) {
            // Если нужно, здесь можно добавить проверку: "А не админ ли группы этот requesterId?"
            throw new RuntimeException("Вы можете удалять только свои сообщения");
        }

        message.setIsDeleted(true);
        // Если хочешь, чтобы вместо текста было написано "Сообщение удалено", раскомментируй:
        // message.setContent("Сообщение удалено");
        // message.setMetadata(null);

        messageRepo.save(message);

        messagingTemplate.convertAndSend(
                "/topic/chat." + message.getConversation().getId(),
                new WsEventDto<>(WsDomain.CHAT,WsEventType.MESSAGE_DELETED.name(), Map.of("messageId", messageId))
        );
    }

    // ==========================================
    // 7. УДАЛЕНИЕ ЧАТА (Скрытие для себя)
    // ==========================================
    @Transactional
    @Override
    public void deleteChatForUser(Long userId, Long conversationId) {
        ConversationMembersEntity membership = memberRepo.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        // Мы не удаляем сообщения! Мы просто скрываем чат из списка пользователя
        membership.setDeleted(true);
        memberRepo.save(membership);
    }

    // ==========================================
    // 8. ВЫХОД ИЗ ГРУППЫ
    // ==========================================
    @Transactional
    @Override
    public void leaveGroup(Long userId, Long conversationId) {
        ConversationsEntity chat = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        if (chat.getType() != ConversationsType.GROUP) {
            throw new RuntimeException("Выйти можно только из группового чата");
        }

        ConversationMembersEntity membership = memberRepo.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Вы не состоите в этой группе"));

        // Физически удаляем связь. Юзер больше не получит сюда сообщения.
        memberRepo.delete(membership);

        // ВАЖНО: Если это был единственный админ, группа останется "сиротой".
        // В будущем здесь стоит добавить логику передачи прав другому участнику,
        // либо удалять группу целиком, если там осталось 0 человек.
    }

    @Transactional
    @Override
    public void toggleChatSettings(Long userId, ChatSettingsRequestDto settings) {
        ConversationMembersEntity membership = memberRepo.findByConversationIdAndUserId(settings.getConversationId(), userId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        if (settings.getIsPinned() != null) membership.setPinned(settings.getIsPinned());
        if (settings.getIsMuted() != null) membership.setMuted(settings.getIsMuted());

        memberRepo.save(membership);
    }

    // ==========================================
    // 10. ИСКЛЮЧЕНИЕ ИЗ ГРУППЫ (Для Админов)
    // ==========================================
    @Transactional
    @Override
    public void kickUser(Long adminId, Long targetUserId, Long conversationId) {
        if (adminId.equals(targetUserId)) {
            throw new IllegalArgumentException("Вы не можете исключить самого себя. Используйте выход из группы.");
        }

        ConversationsEntity chat = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        if (chat.getType() != ConversationsType.GROUP) {
            throw new RuntimeException("Исключать пользователей можно только из групповых чатов");
        }

        // 1. Проверяем права того, кто исключает
        ConversationMembersEntity adminMembership = memberRepo.findByConversationIdAndUserId(conversationId, adminId)
                .orElseThrow(() -> new RuntimeException("Вы не состоите в этой группе"));

        if (adminMembership.getRole() != ConversationMembersRole.ADMIN) {
            throw new RuntimeException("Доступ запрещен: Только администратор может исключать участников");
        }

        // 2. Находим того, кого нужно исключить
        ConversationMembersEntity targetMembership = memberRepo.findByConversationIdAndUserId(conversationId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден в этой группе"));

        if (targetMembership.getRole() == ConversationMembersRole.ADMIN) {
            throw new RuntimeException("Доступ запрещен: Админ не может удалить другого Админа");
        }
        // 3. Удаляем связь (пользователь больше не увидит чат и не получит сообщения)
        memberRepo.delete(targetMembership);
    }

    @Transactional
    @Override
    public ConversationResponseDto addUserToGroup(Long inviterId, List<Long> targetUserIds, Long conversationId) {
        ConversationsEntity chat = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        if (chat.getType() != ConversationsType.GROUP) {
            throw new RuntimeException("Добавлять участников можно только в группы");
        }

        ConversationMembersEntity inviterMember = chat.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(inviterId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Вы не состоите в этой группе"));
        if(inviterMember.getRole() != ConversationMembersRole.ADMIN) {
            throw new RuntimeException("Вы не Админ в этой группе");
        }
        List<Long> alreadyMemberIds = memberRepo.findAllUserIdsInConversation(conversationId, targetUserIds);
        List<Long> idsToAdd = targetUserIds.stream()
                .filter(id -> !alreadyMemberIds.contains(id))
                .distinct()
                .toList();

        // Получаем только тех, кто разрешил себя добавлять/писать
        List<Long> finalIdsToAdd = filterAvailableUsers(inviterId, idsToAdd);

        if (finalIdsToAdd.isEmpty()) {
            throw new RuntimeException("Ни один из выбранных пользователей не может быть добавлен из-за настроек приватности");
        }

        List<AppUserEntity> usersToAdd = userService.getAllEntitiesActiveAndNotLocked(finalIdsToAdd);

        for (AppUserEntity user : usersToAdd) {
            ConversationMembersEntity member = new ConversationMembersEntity();
            member.setId(new ConversationMembersId(chat.getId(), user.getId()));
            member.setConversation(chat);
            member.setUser(user);
            member.setRole(ConversationMembersRole.MEMBER);

            // Добавляем связь с обеих сторон
            chat.getMembers().add(member);
        }

        conversationRepo.save(chat);

        return convertToConversationResponse(chat, false, inviterMember, inviterId);
    }
    @Transactional
    @Override
    public void markConversationAsRead(Long userId, Long conversationId) {
        transactionTemplate.executeWithoutResult(_ -> {
            ConversationMembersEntity membership = memberRepo.findByConversationIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new RuntimeException("Участник не найден"));

            // Устанавливаем текущее время как момент последнего прочтения
            OffsetDateTime now = OffsetDateTime.now();
            membership.setLastReadAt(now);
            memberRepo.save(membership);

            messagingTemplate.convertAndSend(
                    "/topic/user." + userId,
                    new WsEventDto<>(
                            WsDomain.CHAT,
                            WsEventType.READ_UPDATE.name(),
                            Map.of("conversationId", conversationId, "lastReadAt", now)
                    )
            );
        });
    }

}
