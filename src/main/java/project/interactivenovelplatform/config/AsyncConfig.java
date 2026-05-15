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
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Увеличено для лучшей обработки асинхронных задач
        executor.setMaxPoolSize(10); // Увеличено для лучшей обработки асинхронных задач
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("EmailThread-"); // Теперь в логах ты увидишь это имя
        executor.initialize();
        return executor;
    }
}
