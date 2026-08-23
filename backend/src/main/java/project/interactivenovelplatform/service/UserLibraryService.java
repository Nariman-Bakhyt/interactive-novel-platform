package project.interactivenovelplatform.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import project.interactivenovelplatform.dto.request.UserLibraryRequestDto;
import project.interactivenovelplatform.dto.response.UserLibraryResponseDto;
import project.interactivenovelplatform.dto.response.UserLibraryStatusDto;

import java.util.List;

public interface UserLibraryService {
    UserLibraryResponseDto addOrUpdateLibraryEntry(Long currentUserId, UserLibraryRequestDto dto);
    void removeFromLibrary(Long currentUserId, Long novelId);
    PagedModel<UserLibraryResponseDto> getUserLibrary(Long currentUserId, Long targetUserId, Pageable pageable) ;
    List<UserLibraryStatusDto> getUserLibraryStatuses (Long currentUserId);
}
