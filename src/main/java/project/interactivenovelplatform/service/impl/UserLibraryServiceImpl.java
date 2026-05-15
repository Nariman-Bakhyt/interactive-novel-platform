package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.request.UserLibraryRequestDto;
import project.interactivenovelplatform.dto.response.RelationshipStateDto;
import project.interactivenovelplatform.dto.response.UserLibraryResponseDto;
import project.interactivenovelplatform.dto.response.UserLibraryStatusDto;
import project.interactivenovelplatform.entity.PrivacyLevel;
import project.interactivenovelplatform.entity.UserLibraryEntity;
import project.interactivenovelplatform.entity.UserNovelId;
import project.interactivenovelplatform.repository.UserLibraryRepository;
import project.interactivenovelplatform.service.NovelService;
import project.interactivenovelplatform.service.UserLibraryService;
import project.interactivenovelplatform.service.UserService;
import project.interactivenovelplatform.service.UserSocialService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class UserLibraryServiceImpl implements UserLibraryService {
    private final UserLibraryRepository userLibraryRepository;
    private final UserSocialService userSocialService;
    private final UserService userService;
    private final NovelService novelService;

    private UserLibraryResponseDto convertToDto(UserLibraryEntity entity) {
        return new UserLibraryResponseDto(
                entity.getNovel().getId(),
                entity.getNovel().getTitle(),
                entity.getNovel().getCoverUrl(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getPrivacyLevel()
        );
    }
    @Transactional
    @Override
    public UserLibraryResponseDto addOrUpdateLibraryEntry(Long currentUserId, UserLibraryRequestDto dto) {
        var novel =  novelService.getNovelReference(dto.getNovelId());
        UserNovelId id = new UserNovelId(currentUserId , dto.getNovelId());
        UserLibraryEntity entry = userLibraryRepository.findById(id).orElseGet(() -> {
            var newEntry = new UserLibraryEntity();
            newEntry.setId(id);
            newEntry.setUser(userService.getEntityIsActiveAndIsLockedFalse(currentUserId));
            newEntry.setNovel(novel);
            newEntry.setCreatedAt(OffsetDateTime.now());
            return newEntry;
        });
        entry.setStatus(dto.getStatus());
        entry.setPrivacyLevel(dto.getPrivacyLevel());

        return convertToDto(userLibraryRepository.save(entry));
    }
    @Transactional
    @Override
    public void removeFromLibrary(Long currentUserId, Long novelId) {
        UserNovelId id = new UserNovelId(currentUserId , novelId);
        if (!userLibraryRepository.existsById(id)) {
            throw new EntityNotFoundException("Новелла не найдена в вашей библиотеке");
        }
        System.out.println();
        userLibraryRepository.deleteById(id);
    }
    @Transactional(readOnly = true)
    @Override
    public PagedModel<UserLibraryResponseDto> getUserLibrary(Long currentUserId, Long targetUserId, Pageable pageable) {
        if(currentUserId.equals(targetUserId)) {
            var page = userLibraryRepository.findByUserId(targetUserId, pageable);
            return new PagedModel<>(page.map(this::convertToDto));
        }
        RelationshipStateDto relation = userService.getRelationshipState(targetUserId, currentUserId);
        if(relation.isBlockedByMe()){
            throw new AccessDeniedException("Вы находитесь в черном списке пользователя");
        }
        else if (relation.isBlockedByTarget()){
            throw new AccessDeniedException("Пользователь находится в вашем черном списке");
        }

        var settings = userService.getUserSettings(targetUserId);
        PrivacyLevel globalPrivacy = settings.getLibraryPrivacy();
        if (settings.getLibraryPrivacy() == PrivacyLevel.NOBODY) {
        throw new AccessDeniedException("Пользователь скрыл свою библиотеку настройками приватности");
        }


        boolean isFriend = relation.isFriend();
        boolean isBestFriend = isFriend && relation.isBestFriend();
        boolean isFollower = isFriend || relation.isFollowing();

        if (globalPrivacy == PrivacyLevel.BEST_FRIENDS && !isBestFriend) {
            throw new AccessDeniedException("Библиотека доступна только для близких друзей");
        }
        if (globalPrivacy == PrivacyLevel.FRIENDS && !isFriend) {
            throw new AccessDeniedException("Библиотека доступна только для друзей");
        }
        if (globalPrivacy == PrivacyLevel.FOLLOWERS && !isFollower) {
            throw new AccessDeniedException("Библиотека доступна только для подписчиков");
        }
        List<PrivacyLevel> allowedBookLevels = new ArrayList<>();
        allowedBookLevels.add(PrivacyLevel.EVERYONE); // Публичные книги видят все, кто прошел фейс-контроль

        if (isFollower) {
            allowedBookLevels.add(PrivacyLevel.FOLLOWERS);
        }
        if (isFriend) {
            allowedBookLevels.add(PrivacyLevel.FRIENDS);
        }
        if (isBestFriend) {
            allowedBookLevels.add(PrivacyLevel.BEST_FRIENDS);
        }

        Page<UserLibraryEntity> page = userLibraryRepository.findByUserIdAndPrivacyLevelIn(targetUserId, allowedBookLevels, pageable);

        return new PagedModel<>(page.map(this::convertToDto));
    }
    @Transactional(readOnly = true)
    @Override
    public List<UserLibraryStatusDto> getUserLibraryStatuses (Long currentUserId){
        return userLibraryRepository.findAllStatusesByUserId(currentUserId);
    }
}
