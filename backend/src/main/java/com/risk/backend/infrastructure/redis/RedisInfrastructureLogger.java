package com.risk.backend.infrastructure.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动时打印 Redis 连接形态（不脱敏生产密码，仅说明 TLS / Host，便于排障与演示）。
 */
@Component
@Order(15)
public class RedisInfrastructureLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisInfrastructureLogger.class);

    private final Environment env;

    public RedisInfrastructureLogger(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        String host = env.getProperty("spring.data.redis.host", "");
        String port = env.getProperty("spring.data.redis.port", "6379");
        boolean ssl = Boolean.parseBoolean(env.getProperty("spring.data.redis.ssl.enabled", "false"));
        boolean hasPassword = env.getProperty("spring.data.redis.password", "").length() > 0;
        log.info("Redis infrastructure: host={} port={} tls={} auth={}",
                host.isBlank() ? "(default)" : host, port, ssl, hasPassword ? "configured" : "none");
    }
}
