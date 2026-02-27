package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import project.interactivenovelplatform.dto.request.CommentRequestDto;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.AllRatingResponseDto;
import project.interactivenovelplatform.dto.response.AllRatingsResponseDto;
import project.interactivenovelplatform.dto.response.CommentResponseDto;
import project.interactivenovelplatform.dto.response.RatingResponseDto;

public interface CommentService {
    RatingResponseDto setRating(Long novelId, Long userId, RatingRequestDto dto);
    AllRatingsResponseDto getRatings(Long novelId, Pageable pageable);
    RatingResponseDto deleteRating(Long novelId,Long ratingId, Long userId);
    CommentResponseDto createComment(CommentRequestDto dto , String userName);
    CommentResponseDto deleteComment(Long commentId , String userName);
    Page<CommentResponseDto> getComments(CommentRequestDto filter, Pageable pageable);


}
