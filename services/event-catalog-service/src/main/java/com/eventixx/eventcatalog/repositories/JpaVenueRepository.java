package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Venue;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaVenueRepository extends JpaRepository<Venue, UUID>, VenueRepository {

  @Override
  @Modifying
  @Query(
      "UPDATE Venue v SET v.deletedAt = CURRENT_TIMESTAMP, v.deletedBy = :deletedBy,"
          + " v.deletedReason = :deletedReason, v.version = v.version + 1"
          + " WHERE v.id = :id AND v.deletedAt IS NULL")
  void softDelete(
      @Param("id") UUID id,
      @Param("deletedBy") String deletedBy,
      @Param("deletedReason") String deletedReason);
}
