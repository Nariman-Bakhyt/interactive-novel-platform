package project.interactivenovelplatform.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public record UserNovelId(Long userId, Long novelId) implements Serializable {
}
