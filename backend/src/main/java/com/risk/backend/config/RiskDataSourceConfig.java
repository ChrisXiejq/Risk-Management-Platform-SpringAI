package com.risk.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration

@EnableTransactionManagement
public class RiskDataSourceConfig {

    @Bean(name = "riskDataSource")
    public DataSource riskDataSource(ErmProperties props) {
        var c = props.getDatasource();
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(c.getUrl());
        ds.setUsername(c.getUsername());
        ds.setPassword(c.getPassword() == null ? "" : c.getPassword());
        ds.setDriverClassName(c.getDriverClassName());
        ds.setPoolName("erm-pg-pool");
        ds.setMaximumPoolSize(8);
        return ds;
    }

    @Bean(name = "riskJdbcTemplate")
    public NamedParameterJdbcTemplate riskJdbcTemplate(@Qualifier("riskDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean(name = "riskTransactionManager")
    public PlatformTransactionManager riskTransactionManager(@Qualifier("riskDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
