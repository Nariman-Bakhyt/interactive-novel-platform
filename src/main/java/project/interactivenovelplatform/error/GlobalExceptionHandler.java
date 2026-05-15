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

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final static Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. ОШИБКИ ПРИВАТНОСТИ И ДОСТУПА (403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(AccessDeniedException ex) {
        // Пишем как WARN, так как сервер работает нормально, просто юзер лезет куда нельзя
        log.warn("Access denied: {}", ex.getMessage());

        var errorDto = new ErrorResponseDto(
                "Forbidden",
                ex.getMessage(), // Здесь отдавать message безопасно (наши кастомные строки)
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDto);
    }

    // 2. ОШИБКИ ВАЛИДАЦИИ DTO (400 Bad Request) - например, если сработал @NotBlank
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Собираем все ошибки валидации в одну читаемую строку
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errorMessage);

        var errorDto = new ErrorResponseDto(
                "Validation Error",
                errorMessage,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    // 3. ОШИБКИ БИЗНЕС-ЛОГИКИ (400 Bad Request)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponseDto> handleBusinessLogicExceptions(RuntimeException ex) {
        log.warn("Business logic error: {}", ex.getMessage());

        var errorDto = new ErrorResponseDto(
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    // 4. НЕПРЕДВИДЕННЫЕ ОШИБКИ - ФИНАЛЬНЫЙ ПЕРЕХВАТЧИК (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        // ВАЖНО: Передаем весь объект 'ex' в лог, чтобы напечатался полный Stack Trace!
        log.error("Unhandled exception occurred", ex);

        var errorDto = new ErrorResponseDto(
                "Internal Server Error",
                // ВАЖНО: Прячем ex.getMessage() от фронтенда!
                // Иначе клиент может увидеть SQL-запросы или пути к файлам сервера.
                "Произошла непредвиденная ошибка на сервере. Пожалуйста, повторите попытку позже.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("Файл слишком большой! Максимальный размер — 20МБ");
    }

}
