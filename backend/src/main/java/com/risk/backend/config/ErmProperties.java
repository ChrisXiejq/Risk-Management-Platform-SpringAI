package com.risk.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.erm")
public class ErmProperties {

    /** 启动时执行 db/erm-schema.sql */
    private boolean schemaInit = false;

    private final Jwt jwt = new Jwt();

    private final DataSourceProps datasource = new DataSourceProps();

    @Data
    public static class Jwt {
        private String secret = "";
        private int accessHours = 24;
    }

    @Data
    public static class DataSourceProps {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";
    }
}
