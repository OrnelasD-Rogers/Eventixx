package com.eventixx.searchservice.services;

import com.eventixx.searchservice.repositories.EventDocument;
import com.eventixx.searchservice.repositories.EventDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventIndexService {

  private final EventDocumentRepository repository;
  private final EventDocumentMapper mapper;

  /** Indexes an event from a Kafka payload into Elasticsearch. */
  public void indexEvent(com.eventixx.searchservice.services.messaging.EventPublished event) {
    EventDocument document = mapper.toDocument(event);
    repository.save(document);
    log.info("Indexed event {} in Elasticsearch", document.getEventId());
  }
}
