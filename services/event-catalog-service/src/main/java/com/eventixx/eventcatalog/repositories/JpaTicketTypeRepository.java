package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.TicketType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaTicketTypeRepository
    extends JpaRepository<TicketType, UUID>, TicketTypeRepository {

  @Override
  List<TicketType> findAllByEventId(UUID eventId);

  @Override
  @Modifying
  @Query(
      "UPDATE TicketType t SET t.deletedAt = CURRENT_TIMESTAMP, t.deletedBy = :deletedBy,"
          + " t.deletedReason = :deletedReason, t.version = t.version + 1"
          + " WHERE t.id = :id AND t.deletedAt IS NULL")
  void softDelete(
      @Param("id") UUID id,
      @Param("deletedBy") String deletedBy,
      @Param("deletedReason") String deletedReason);
}
