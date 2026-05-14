package com.eventixx.eventcatalog.services.tickettype;

import com.eventixx.eventcatalog.config.MapStructConfig;
import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.dto.tickettype.TicketTypeResponse;
import com.eventixx.eventcatalog.dto.tickettype.TicketTypeSummaryResponse;
import com.eventixx.eventcatalog.dto.tickettype.UpdateTicketTypeRequest;
import com.eventixx.eventcatalog.entities.TicketType;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface TicketTypeMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "event", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  TicketType toEntity(CreateTicketTypeRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "event", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntity(UpdateTicketTypeRequest request, @MappingTarget TicketType ticketType);

  TicketTypeResponse toResponse(TicketType ticketType);

  TicketTypeSummaryResponse toSummary(TicketType ticketType);

  List<TicketTypeResponse> toResponseList(List<TicketType> ticketTypes);

  List<TicketTypeSummaryResponse> toSummaryList(List<TicketType> ticketTypes);
}
