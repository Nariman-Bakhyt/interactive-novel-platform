package project.interactivenovelplatform.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // Разрешаем вешать только на методы контроллеров
@Retention(RetentionPolicy.RUNTIME) // Чтобы Spring видел её во время работы
public @interface RateLimited {
    // Это параметры, которые мы сможем настраивать
    int capacity() default 10;   // Количество запросов
    int minutes() default 1;    // Интервал времени
}
