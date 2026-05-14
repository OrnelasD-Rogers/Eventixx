package com.eventixx.searchservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.eventixx.searchservice.dto.SearchCriteria;
import com.eventixx.searchservice.dto.SearchResult;
import com.eventixx.searchservice.repositories.EventDocument;
import com.eventixx.searchservice.repositories.EventDocumentRepository;
import com.eventixx.searchservice.services.SearchService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest
@Testcontainers
@Disabled("Requires Docker daemon to be running")
class SearchServiceIntegrationTest {

  @Container
  static ElasticsearchContainer elasticsearch =
      new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.2.8")
          .withEnv("xpack.security.enabled", "false");

  @Container static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }

  @Autowired private EventDocumentRepository repository;

  @Autowired private SearchService searchService;

  @Test
  void shouldIndexAndSearchEvents() {
    var doc1 =
        EventDocument.builder()
            .eventId(UUID.randomUUID().toString())
            .title("Rock Concert")
            .description("A great rock concert")
            .categoryName("Music")
            .venueName("Arena")
            .city("Sao Paulo")
            .country("Brazil")
            .startTime(Instant.now())
            .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
            .minPrice(BigDecimal.valueOf(50))
            .maxPrice(BigDecimal.valueOf(150))
            .ticketTypes(List.of())
            .build();

    var doc2 =
        EventDocument.builder()
            .eventId(UUID.randomUUID().toString())
            .title("Theater Play")
            .description("A dramatic theater play")
            .categoryName("Theater")
            .venueName("Theater Hall")
            .city("Sao Paulo")
            .country("Brazil")
            .startTime(Instant.now().plus(1, ChronoUnit.DAYS))
            .endTime(Instant.now().plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS))
            .minPrice(BigDecimal.valueOf(30))
            .maxPrice(BigDecimal.valueOf(80))
            .ticketTypes(List.of())
            .build();

    repository.save(doc1);
    repository.save(doc2);

    var criteria = new SearchCriteria("rock", null, null, null, null, null, null, null, null, 20);
    SearchResult result = searchService.search(criteria);

    assertThat(result.events()).isNotEmpty();
    assertThat(result.events()).anyMatch(e -> e.title().contains("Rock"));
  }
}
