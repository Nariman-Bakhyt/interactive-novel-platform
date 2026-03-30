package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.CommentRequestDto;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.AllRatingResponseDto;
import project.interactivenovelplatform.dto.response.AllRatingsResponseDto;
import project.interactivenovelplatform.dto.response.CommentResponseDto;
import project.interactivenovelplatform.dto.response.RatingResponseDto;
import project.interactivenovelplatform.entity.CommentEntity;
import project.interactivenovelplatform.entity.Metadata;
import project.interactivenovelplatform.security.UserPrincipal;

import java.util.List;

public interface CommentService {
    RatingResponseDto setRating(Long novelId, Long userId, RatingRequestDto dto);
    AllRatingsResponseDto getRatings(Long novelId, Pageable pageable);
    RatingResponseDto deleteRating(Long novelId,Long ratingId, Long userId);
    CommentResponseDto createComment(List<MultipartFile> files, CommentRequestDto dto , UserPrincipal principal);
    CommentResponseDto deleteComment(Long commentId , String userName);
    Page<CommentResponseDto> getComments(CommentRequestDto filter, Pageable pageable);

}
