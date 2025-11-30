package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.dto.request.NovelRequestDto;
import project.interactivenovelplatform.dto.request.NovelUpdateRequestDto;
import project.interactivenovelplatform.dto.response.NovelResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.service.NovelService;
import project.interactivenovelplatform.service.UserService;

@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {
    private final NovelService novelService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<NovelResponseDto>> findAllNovels(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NovelResponseDto> novels = novelService.findAll(pageable);
        return ResponseEntity.ok(novels);
    }
    @GetMapping("/{id}")
    public ResponseEntity<NovelResponseDto> findNovelById(@PathVariable Long id) {
        NovelResponseDto novel = novelService.findById(id);
        return ResponseEntity.ok(novel);
    }


    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NovelResponseDto> createNovel(
            @RequestBody @Valid NovelRequestDto novelRequestDto,
            Authentication authentication
    ) {
        var currentAuthorId = userService.findByUsername(authentication.getName()).getId();
        NovelResponseDto createdNovel = novelService.create(novelRequestDto, currentAuthorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNovel);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@novelServiceImpl.isAuthor(#id, authentication.name) or hasRole('ADMIN')")
    public ResponseEntity<NovelResponseDto> updateNovel(
            @PathVariable Long id,
            @RequestBody NovelUpdateRequestDto novelRequestDto,
            Authentication authentication
    ) {
        var currentAuthorId = userService.findByUsername(authentication.getName()).getId();
        NovelResponseDto updatedNovel = novelService.update(id, novelRequestDto, currentAuthorId);
        return ResponseEntity.ok(updatedNovel);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@novelServiceImpl.isAuthor(#id, authentication.name) or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'THE_MAKER')")
    public ResponseEntity<NovelResponseDto> changeNovelStatus(
            @PathVariable Long id,
            @RequestBody NovelUpdateRequestDto newStatus,
            Authentication authentication
    ) {
        var currentAuthorId = userService.findByUsername(authentication.getName()).getId();
        NovelResponseDto changedNovel = novelService.changeStatus(id, currentAuthorId, newStatus);
        return ResponseEntity.ok(changedNovel);
    }
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> deleteNovel(@PathVariable Long id) {
//        novelService.delete(id);
//        return ResponseEntity.noContent().build();
//    }
}
