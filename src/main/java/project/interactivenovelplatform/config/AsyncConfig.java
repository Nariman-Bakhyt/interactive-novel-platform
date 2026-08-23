package project.interactivenovelplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        // Настраиваем кастомный пул потоков для асинхронных фоновых задач (например, рассылки почты).
        // Ограничение пула (Core 5, Max 10) и очередь в 500 задач защищают JVM от перерасхода памяти и OOM при пиковых нагрузках.
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); 
        executor.setMaxPoolSize(10); 
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("EmailThread-"); 
        executor.initialize();
        return executor;
    }
}
