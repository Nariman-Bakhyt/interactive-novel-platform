package project.interactivenovelplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.config.RateLimited;
import project.interactivenovelplatform.dto.request.CommentRequestDto;
import project.interactivenovelplatform.dto.response.CommentResponseDto;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.CommentService;
import project.interactivenovelplatform.service.NovelService;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;
    private final NovelService novelService;
    private final SimpMessagingTemplate messagingTemplate;

    @RateLimited(capacity = 100, minutes = 1)
    @GetMapping("/public")
    public ResponseEntity<PagedModel<CommentResponseDto>> getComments(CommentRequestDto commentRequestDto,
                                                                        @PageableDefault(size = 20, sort = "timestamp",
                                                                        direction = Sort.Direction.DESC) Pageable pageable)
    {
        Page<CommentResponseDto> page = commentService.getComments(commentRequestDto, pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @RateLimited(capacity = 10, minutes = 1)
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponseDto> createComment(@RequestPart(value = "files", required = false) List<MultipartFile> files
            ,@RequestPart("comment") CommentRequestDto commentRequestDto, @AuthenticationPrincipal UserPrincipal principal){
        CommentResponseDto response = commentService.createComment(files,commentRequestDto, principal);
        String topic = determineTopic(response);
        messagingTemplate.convertAndSend(topic, (Object) response);
        return ResponseEntity.ok(response);
    }

    private String determineTopic(CommentResponseDto response) {
        if (response.getBlockId() != null) return "/topic/block." + response.getBlockId();
        if (response.getChapterId() != null) return "/topic/chapter." + response.getChapterId();
        if (response.getNovelId() != null) return "/topic/novel." + response.getNovelId();
        return "/topic/global";
    }
    @RateLimited(capacity = 10, minutes = 1)
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, Principal principal){
        CommentResponseDto deletedComment = commentService.deleteComment(commentId, principal.getName());

        String destination = determineTopic(deletedComment);
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", commentId);
        payload.put("deleted", true);

        messagingTemplate.convertAndSend(destination, (Object)payload);
        return ResponseEntity.ok().build();
    }

}
