package project.interactivenovelplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.interactivenovelplatform.dto.request.CommentRequestDto;
import project.interactivenovelplatform.dto.response.CommentResponseDto;
import project.interactivenovelplatform.service.CommentService;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/public")
    public ResponseEntity<Page<CommentResponseDto>> getComments(CommentRequestDto commentRequestDto,
                                                                        @PageableDefault(size = 20, sort = "id",
                                                                        direction = Sort.Direction.DESC) Pageable pageable)
    {
        return ResponseEntity.ok(commentService.getComments(commentRequestDto, pageable));
    }

    @MessageMapping("/comment.send")
    public void createComment(CommentRequestDto commentRequestDto, Principal principal){
        CommentResponseDto response = commentService.createComment(commentRequestDto, principal.getName());
        String topic = determineTopic(response);
        messagingTemplate.convertAndSend(topic, response);

    }

    private String determineTopic(CommentResponseDto response) {
        if (response.getBlockId() != null) return "/topic/block." + response.getBlockId();
        if (response.getChapterId() != null) return "/topic/chapter." + response.getChapterId();
        if (response.getNovelId() != null) return "/topic/novel." + response.getNovelId();
        return "/topic/global";
    }

}
