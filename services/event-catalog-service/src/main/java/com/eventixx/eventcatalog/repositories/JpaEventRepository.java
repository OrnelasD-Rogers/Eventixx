package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Event;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaEventRepository extends JpaRepository<Event, UUID>, EventRepository {

  @Override
  @Modifying
  @Query(
      "UPDATE Event e SET e.deletedAt = CURRENT_TIMESTAMP, e.deletedBy = :deletedBy,"
          + " e.deletedReason = :deletedReason, e.version = e.version + 1"
          + " WHERE e.id = :id AND e.deletedAt IS NULL")
  void softDelete(
      @Param("id") UUID id,
      @Param("deletedBy") String deletedBy,
      @Param("deletedReason") String deletedReason);

  @Override
  boolean existsByVenueId(UUID venueId);
}
