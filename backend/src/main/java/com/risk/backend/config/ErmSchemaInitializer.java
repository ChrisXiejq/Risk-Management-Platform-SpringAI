package com.risk.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * 可选：首次部署时自动建表（生产建议关闭，改用迁移工具）。
 */
@Component
@Order(5)
public class ErmSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ErmSchemaInitializer.class);

    private final ErmProperties props;
    private final javax.sql.DataSource riskDataSource;

    public ErmSchemaInitializer(ErmProperties props, @Qualifier("riskDataSource") javax.sql.DataSource riskDataSource) {
        this.props = props;
        this.riskDataSource = riskDataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!props.isSchemaInit()) {
            return;
        }
        ClassPathResource res = new ClassPathResource("db/erm-schema.sql");
        if (!res.exists()) {
            log.warn("ERM schema script missing: db/erm-schema.sql");
            return;
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(res);
        populator.setContinueOnError(false);
        populator.execute(riskDataSource);
        log.info("ERM schema applied (app.erm.schema-init=true)");
    }
}
