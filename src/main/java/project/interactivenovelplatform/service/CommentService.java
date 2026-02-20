package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import project.interactivenovelplatform.dto.request.CommentRequestDto;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.CommentResponseDto;
import project.interactivenovelplatform.dto.response.RatingStatsDto;

public interface CommentService {
    RatingStatsDto setRating(Long novelId, Long userId, RatingRequestDto dto);
    RatingStatsDto  deleteRating(Long novelId, Long userId);
    CommentResponseDto createComment(CommentRequestDto dto , String userName);
    Page<CommentResponseDto> getComments(CommentRequestDto filter, Pageable pageable);
}
