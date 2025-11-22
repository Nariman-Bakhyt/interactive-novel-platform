package project.interactivenovelplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Novel {
    RETRACTED("Отозвано автором"),
    ARCHIVED("Архивирован автором"),
    DRAFT("Черновик"),
    COMPLETED("завершенный"),
    IN_PROGRESS("в процессе"),
    HIATUS("перерыв");

    private final String novelStatus;

}
