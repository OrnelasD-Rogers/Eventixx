package com.eventixx.eventcatalog.integration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.event.CreateEventRequest;
import com.eventixx.eventcatalog.dto.event.EventResponse;
import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.dto.venue.CreateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.VenueResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Base class for catalog integration tests with shared PostgreSQL and Kafka Testcontainers. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
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

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Consumer<String, String> kafkaTestConsumer;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE ticket_types, events, venues, categories RESTART IDENTITY CASCADE");
    }

    protected static HttpEntity<Void> authEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "user-1");
        return new HttpEntity<>(headers);
    }

    protected VenueResponse createVenue() {
        var request = new CreateVenueRequest("Test Venue", "123 Street", "City", "Country", 100);
        ResponseEntity<VenueResponse> response = restTemplate.exchange(
                "/api/v1/venues", HttpMethod.POST,
                new HttpEntity<>(request, authEntity().getHeaders()),
                VenueResponse.class);
        return response.getBody();
    }

    protected CategoryResponse createCategory(int index) {
        ResponseEntity<CategoryResponse> response = restTemplate.exchange(
                "/api/v1/categories", HttpMethod.POST,
                new HttpEntity<>(
                        new CreateCategoryRequest("Test Category_%d".formatted(index), "Description"),
                        authEntity().getHeaders()),
                CategoryResponse.class);
        return response.getBody();
    }

    protected CreateEventRequest buildCreateRequest(UUID venueId, UUID categoryId,
                                                    List<CreateTicketTypeRequest> ticketTypes) {
        Instant future = Instant.now().plus(7, ChronoUnit.DAYS);
        return new CreateEventRequest("Test Event", "Description",
                venueId, categoryId, future, future.plus(3, ChronoUnit.HOURS), ticketTypes);
    }

    protected CreateEventRequest buildCreateRequest(UUID venueId, UUID categoryId) {
        return buildCreateRequest(venueId, categoryId, List.of(
                new CreateTicketTypeRequest("Standard", BigDecimal.valueOf(50), 50)));
    }

    protected EventResponse createEvent(UUID venueId, UUID categoryId) {
        ResponseEntity<EventResponse> response = restTemplate.exchange(
                "/api/v1/events", HttpMethod.POST,
                new HttpEntity<>(buildCreateRequest(venueId, categoryId), authEntity().getHeaders()),
                EventResponse.class);
        return response.getBody();
    }

    @AfterEach
    void tearDownKafkaConsumer() {
        if (kafkaTestConsumer != null) {
            kafkaTestConsumer.close();
        }
    }

    protected Consumer<String, String> createKafkaConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                KAFKA.getBootstrapServers(), "test-group-" + UUID.randomUUID(), false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put("metadata.max.age.ms", 1000);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer());
        kafkaTestConsumer = cf.createConsumer();
        kafkaTestConsumer.subscribe(List.of("event.published"));
        return kafkaTestConsumer;
    }
}
