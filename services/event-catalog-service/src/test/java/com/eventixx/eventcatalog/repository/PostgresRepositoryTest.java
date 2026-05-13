package com.eventixx.eventcatalog.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for repository tests using a shared PostgreSQL Testcontainers instance.
 * <p>
 * Uses {@code @DataJpaTest} with explicit {@code @ImportAutoConfiguration} to include
 * Flyway. {@code @DataJpaTest} is a slice test that excludes Flyway by default,
 * causing schema validation failures.
 * <p>
 * The container is started via a static initializer block so it is shared across all
 * subclasses and never restarted between test classes. This avoids connection failures
 * caused by JUnit 5 lifecycle conflicts with {@code @Testcontainers} and static fields.
 * <p>
 * Uses {@code @DynamicPropertySource} instead of {@code @ServiceConnection} for
 * explicit and reliable container wiring with slice tests.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/5766">DataJpaTest does not invoke flyway</a>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration({
    FlywayAutoConfiguration.class
})
@ActiveProfiles("test")
public abstract class PostgresRepositoryTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
