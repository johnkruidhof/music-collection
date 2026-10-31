package nl.johnkruidhof.musiccollection.service;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.wishlist.AddToWishlistRequest;
import nl.johnkruidhof.musiccollection.dto.wishlist.WishlistItemDto;
import nl.johnkruidhof.musiccollection.entity.AppUser;
import nl.johnkruidhof.musiccollection.entity.CollectionItem;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.entity.WishlistItem;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.mapper.CollectionItemMapper;
import nl.johnkruidhof.musiccollection.mapper.WishlistMapper;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;
import nl.johnkruidhof.musiccollection.repository.WishlistItemRepository;

/**
 * Verlanglijst (F-24): releases uit Discogs/MusicBrainz die je nog niet hebt. Toevoegen is idempotent
 * (dezelfde release twee keer = de bestaande wens). "Gekocht" verplaatst een wens naar de collectie.
 */
@Service
@RequiredArgsConstructor
public class WishlistService {

    static final String RESOURCE = "Wens";

    private final WishlistItemRepository wishlistItemRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final ReleaseRepository releaseRepository;
    private final CatalogService catalogService;
    private final UserService userService;
    private final WishlistMapper wishlistMapper;
    private final CollectionItemMapper collectionItemMapper;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public List<WishlistItemDto> list(Jwt jwt) {
        return wishlistItemRepository.findByUserIdOrderByAddedAtDesc(userService.getOrCreate(jwt).getId())
                .stream().map(wishlistMapper::toDto).toList();
    }

    /** Release eerst (buiten de transactie) uit de catalogus of de bron, net als bij de collectie. */
    public WishlistItemDto add(Jwt jwt, AddToWishlistRequest request) {
        UUID releaseId = catalogService.getOrImport(request.source(), request.externalId().strip());
        try {
            return transactionTemplate.execute(status -> addInTransaction(jwt, releaseId, request.notes()));
        } catch (DataIntegrityViolationException ex) {
            // Tegelijk twee keer toegevoegd: de unieke index won, geef de bestaande terug.
            return transactionTemplate.execute(status -> wishlistMapper.toDto(wishlistItemRepository
                    .findByUserIdAndReleaseId(userService.getOrCreate(jwt).getId(), releaseId).orElseThrow()));
        }
    }

    private WishlistItemDto addInTransaction(Jwt jwt, UUID releaseId, String notes) {
        AppUser user = userService.getOrCreate(jwt);
        return wishlistItemRepository.findByUserIdAndReleaseId(user.getId(), releaseId)
                .map(wishlistMapper::toDto)
                .orElseGet(() -> {
                    Release release = releaseRepository.findById(releaseId)
                            .orElseThrow(() -> new ResourceNotFoundException("Release", releaseId));
                    WishlistItem item = new WishlistItem(user, release);
                    item.setNotes(notes == null || notes.isBlank() ? null : notes.strip());
                    return wishlistMapper.toDto(wishlistItemRepository.saveAndFlush(item));
                });
    }

    @Transactional
    public void delete(Jwt jwt, UUID id) {
        wishlistItemRepository.delete(findOwned(jwt, id));
    }

    /** Gekocht: het wordt een collectie-item (eigen velden optioneel) en verdwijnt van de verlanglijst. */
    @Transactional
    public CollectionItemDto acquire(Jwt jwt, UUID id, CollectionItemFields fields) {
        WishlistItem wish = findOwned(jwt, id);
        CollectionItem item = new CollectionItem(wish.getUser(), wish.getRelease());
        collectionItemMapper.applyFields(fields == null ? CollectionItemFields.EMPTY : fields, item);
        CollectionItem saved = collectionItemRepository.saveAndFlush(item);
        wishlistItemRepository.delete(wish);
        return collectionItemMapper.toDto(saved);
    }

    private WishlistItem findOwned(Jwt jwt, UUID id) {
        return wishlistItemRepository.findByIdAndUserId(id, userService.getOrCreate(jwt).getId())
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}
