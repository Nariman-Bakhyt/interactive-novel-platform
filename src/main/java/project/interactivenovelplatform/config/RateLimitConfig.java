package project.interactivenovelplatform.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RateLimitConfig {
    @Bean
    public RedisClient redisClient(RedisConnectionFactory factory) {
        if (factory instanceof LettuceConnectionFactory lettuceFactory) {
            return (RedisClient) lettuceFactory.getNativeClient();
        }
        throw new IllegalStateException("Required LettuceConnectionFactory");
    }

    @Bean
    public StatefulRedisConnection<byte[], byte[]> bucket4jRedisConnection(RedisClient redisClient) {
        return redisClient.connect(new ByteArrayCodec());
    }

    @Bean
    public LettuceBasedProxyManager<byte[]> proxyManager(StatefulRedisConnection<byte[], byte[]> connection) {
        return Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(5)))
                .build();
    }

}
