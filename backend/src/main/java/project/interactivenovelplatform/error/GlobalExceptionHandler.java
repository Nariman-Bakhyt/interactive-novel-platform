package project.interactivenovelplatform.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final static Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(AccessDeniedException ex) {
        
        log.warn("Access denied: {}", ex.getMessage());

        var errorDto = new ErrorResponseDto(
                ex.getMessage() != null ? ex.getMessage() : "Forbidden",
                ex.getMessage(), 
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDto);
    }

    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getField() + ": " + error.getCode())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errorMessage);

        var errorDto = new ErrorResponseDto(
                errorMessage,
                errorMessage,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, DuplicateKeyException.class})
    public ResponseEntity<ErrorResponseDto> handleBusinessLogicExceptions(RuntimeException ex) {
        log.warn("Business logic error: {}", ex.getMessage());

        var errorDto = new ErrorResponseDto(
                ex.getMessage(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        
        log.error("Unhandled exception occurred", ex);

        var errorDto = new ErrorResponseDto(
                "Internal Server Error",
                
                
                "Произошла непредвиденная ошибка на сервере. Пожалуйста, повторите попытку позже.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body("Файл слишком большой! Максимальный размер — 20МБ");
    }

}
