package nl.johnkruidhof.musiccollection.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {

    List<Release> findByBarcode(String barcode);

    List<Release> findByBarcodeInAndSourceNot(Collection<String> barcodes, MetadataSource source);

    Optional<Release> findByDiscogsId(String discogsId);

    Optional<Release> findByMusicbrainzId(String musicbrainzId);
}
