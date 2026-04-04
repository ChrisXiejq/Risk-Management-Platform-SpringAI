package com.risk.backend.infrastructure.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis / Upstash 与缓存策略（业务层可调 TTL，便于面试说明「边缘缓存 + 失效」）。
 */
@Data
@ConfigurationProperties(prefix = "app.redis")
public class AppRedisProperties {

    private final Upstash upstash = new Upstash();
    private final Cache cache = new Cache();

    @Data
    public static class Upstash {
        /** 云端托管端点，如 xxx.upstash.io */
        private String host = "";
        private int port = 6379;
        private String username = "default";
        /** 等价于 redis-cli -u 中的密码段；勿提交到 Git，见 application-local.yaml */
        private String token = "";
    }

    @Data
    public static class Cache {
        /** 工作台总览聚合查询缓存时间 */
        private Duration dashboardOverviewTtl = Duration.ofSeconds(90);
    }
}
