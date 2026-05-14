package com.eventixx.eventcatalog.integration;

import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.event.CreateEventRequest;
import com.eventixx.eventcatalog.dto.event.EventResponse;
import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.dto.venue.CreateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.VenueResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for catalog integration tests.
 * Provides singleton Testcontainers (PostgreSQL + Kafka), {@link RestTestClient}, DB cleanup, and helper methods.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
public abstract class CatalogIntegrationTestBase {

  static final PostgreSQLContainer POSTGRES;
  static final KafkaContainer KAFKA;

  static {
    POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    KAFKA = new KafkaContainer("apache/kafka:4.0.0");
    POSTGRES.start();
    KAFKA.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
  }

  @Autowired protected RestTestClient restClient;

  @Autowired private JdbcTemplate jdbcTemplate;

  private Consumer<String, String> kafkaTestConsumer;

  @BeforeEach
  void cleanDatabase() {
    jdbcTemplate.execute(
        "TRUNCATE TABLE ticket_types, events, venues, categories RESTART IDENTITY CASCADE");
  }

  protected static java.util.function.Consumer<HttpHeaders> requestHeaders() {
    return headers -> headers.set("X-User-Id", "user-1");
  }

  protected VenueResponse createVenue() {
    var request = new CreateVenueRequest("Test Venue", "123 Street", "City", "Country", 100);
    return restClient
        .post()
        .uri("/api/v1/venues")
        .headers(requestHeaders())
        .body(request)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(VenueResponse.class)
        .returnResult()
        .getResponseBody();
  }

  protected CategoryResponse createCategory(int index) {
    var request = new CreateCategoryRequest("Test Category_%d".formatted(index), "Description");
    return restClient
        .post()
        .uri("/api/v1/categories")
        .headers(requestHeaders())
        .body(request)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(CategoryResponse.class)
        .returnResult()
        .getResponseBody();
  }

  protected CreateEventRequest buildCreateRequest(
      UUID venueId, UUID categoryId, List<CreateTicketTypeRequest> ticketTypes) {
    Instant future = Instant.now().plus(7, ChronoUnit.DAYS);
    return new CreateEventRequest(
        "Test Event",
        "Description",
        venueId,
        categoryId,
        future,
        future.plus(3, ChronoUnit.HOURS),
        ticketTypes);
  }

  protected CreateEventRequest buildCreateRequest(UUID venueId, UUID categoryId) {
    return buildCreateRequest(
        venueId,
        categoryId,
        List.of(new CreateTicketTypeRequest("Standard", BigDecimal.valueOf(50), 50)));
  }

  protected EventResponse createEvent(UUID venueId, UUID categoryId) {
    return restClient
        .post()
        .uri("/api/v1/events")
        .headers(requestHeaders())
        .body(buildCreateRequest(venueId, categoryId))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(EventResponse.class)
        .returnResult()
        .getResponseBody();
  }

  @AfterEach
  void tearDownKafkaConsumer() {
    if (kafkaTestConsumer != null) {
      kafkaTestConsumer.close();
    }
  }

  protected Consumer<String, String> createKafkaConsumer() {
    Map<String, Object> props =
        KafkaTestUtils.consumerProps(
            KAFKA.getBootstrapServers(), "test-group-" + UUID.randomUUID(), false);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put("metadata.max.age.ms", 1000);
    DefaultKafkaConsumerFactory<String, String> cf =
        new DefaultKafkaConsumerFactory<>(
            props, new StringDeserializer(), new StringDeserializer());
    kafkaTestConsumer = cf.createConsumer();
    kafkaTestConsumer.subscribe(List.of("event.published"));
    return kafkaTestConsumer;
  }
}
