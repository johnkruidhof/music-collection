package nl.johnkruidhof.musiccollection.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import nl.johnkruidhof.musiccollection.entity.CoverImage;

public interface CoverImageRepository extends JpaRepository<CoverImage, UUID> {

    @Modifying(flushAutomatically = true)
    @Query("delete from CoverImage c where c.releaseId = :releaseId")
    int deleteByReleaseId(UUID releaseId);
}
