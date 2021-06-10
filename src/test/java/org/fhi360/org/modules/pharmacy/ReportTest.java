package org.fhi360.org.modules.pharmacy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
public class ReportTest {
    private final static JdbcTemplate JDBC_TEMPLATE;

    @Test
    public void testReport() {
        LOG.info("Patients: {}", RegExUtils.replaceAll("04049_04/04/1/3/1/0001", "/", "-"));
    }

    static {
        org.postgresql.ds.PGPoolingDataSource ds = new org.postgresql.ds.PGPoolingDataSource();
        ds.setUrl("jdbc:postgresql://localhost/facility");
        ds.setUser("postgres");
        ds.setPassword("lamis");
        /* the connection pool will have 10 to 20 connections */
        ds.setInitialConnections(10);
        ds.setMaxConnections(20);
        /* use SSL connections without checking server certificate */
        ds.setSslMode("require");
        ds.setSslfactory("org.postgresql.ssl.NonValidatingFactory");
        JDBC_TEMPLATE = new JdbcTemplate(ds);
    }
}
