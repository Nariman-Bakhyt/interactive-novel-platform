package project.interactivenovelplatform.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.config.RateLimited;
import project.interactivenovelplatform.dto.request.CommentRequestDto;
import project.interactivenovelplatform.dto.response.CommentResponseDto;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.CommentService;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    @RateLimited(capacity = 100, minutes = 1)
    @GetMapping("/public")
    public ResponseEntity<Slice<CommentResponseDto>> getComments(CommentRequestDto commentRequestDto,
                                                                        @PageableDefault(size = 20, sort = "timestamp",
                                                                        direction = Sort.Direction.DESC) Pageable pageable)
    {
        if (pageable.getPageSize() > 50) {
            pageable = PageRequest.of(pageable.getPageNumber(), 50, pageable.getSort());
        }
        Slice<CommentResponseDto> page = commentService.getComments(commentRequestDto, pageable);
        return ResponseEntity.ok(page);
    }

    @RateLimited(capacity = 1000, minutes = 1)
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponseDto> createComment(@RequestPart(value = "files", required = false) List<MultipartFile> files
            ,@RequestPart("comment") @Valid CommentRequestDto commentRequestDto, @AuthenticationPrincipal UserPrincipal principal){
        CommentResponseDto response = commentService.createComment(files,commentRequestDto, principal.getId());
        return ResponseEntity.ok(response);
    }
    @RateLimited(capacity = 10, minutes = 1)
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, Principal principal){
        commentService.deleteComment(commentId, principal.getName());
        return ResponseEntity.ok().build();
    }

}