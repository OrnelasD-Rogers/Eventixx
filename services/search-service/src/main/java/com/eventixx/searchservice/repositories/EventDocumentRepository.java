package com.eventixx.searchservice.repositories;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/** Spring Data Elasticsearch repository for {@link EventDocument}. */
public interface EventDocumentRepository extends ElasticsearchRepository<EventDocument, String> {}
