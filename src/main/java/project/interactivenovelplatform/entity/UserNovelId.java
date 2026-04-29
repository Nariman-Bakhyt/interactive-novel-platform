package project.interactivenovelplatform.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
public record UserNovelId(Long userId, Long novelId) implements Serializable {
}
