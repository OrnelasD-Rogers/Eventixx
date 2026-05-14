package com.eventixx.searchservice.services;

import com.eventixx.searchservice.config.MapStructConfig;
import com.eventixx.searchservice.repositories.EventDocument;
import com.eventixx.searchservice.services.messaging.EventPublished;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper between Kafka event payloads and Elasticsearch documents. */
@Mapper(config = MapStructConfig.class)
public interface EventDocumentMapper {

  /** Maps an EventPublished Kafka payload to an EventDocument for Elasticsearch indexing. */
  @Mapping(target = "eventId", source = "eventId")
  @Mapping(target = "ticketTypes", source = "ticketTypes")
  @Mapping(target = "minPrice", expression = "java(mapMinPrice(event.ticketTypes()))")
  @Mapping(target = "maxPrice", expression = "java(mapMaxPrice(event.ticketTypes()))")
  EventDocument toDocument(EventPublished event);

  /** Extracts the minimum price from a list of ticket types. */
  default BigDecimal mapMinPrice(List<EventPublished.TicketTypeInfo> ticketTypes) {
    if (ticketTypes == null || ticketTypes.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return ticketTypes.stream()
        .map(EventPublished.TicketTypeInfo::price)
        .min(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);
  }

  /** Extracts the maximum price from a list of ticket types. */
  default BigDecimal mapMaxPrice(List<EventPublished.TicketTypeInfo> ticketTypes) {
    if (ticketTypes == null || ticketTypes.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return ticketTypes.stream()
        .map(EventPublished.TicketTypeInfo::price)
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);
  }

  List<EventDocument.TicketTypeDocument> toTicketDocumentList(
      List<EventPublished.TicketTypeInfo> ticketTypes);

  EventDocument.TicketTypeDocument toTicketDocument(EventPublished.TicketTypeInfo ticketType);
}
