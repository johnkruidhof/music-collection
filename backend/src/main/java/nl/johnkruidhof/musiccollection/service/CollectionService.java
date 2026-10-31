package nl.johnkruidhof.musiccollection.service;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.collection.AddToCollectionRequest;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionPageDto;
import nl.johnkruidhof.musiccollection.entity.AppUser;
import nl.johnkruidhof.musiccollection.entity.CollectionItem;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.mapper.CollectionItemMapper;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.CollectionItemSpecifications;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/** De persoonlijke collectie. Alles is gescoped op de ingelogde gebruiker; andermans item = 404. */
@Service
@RequiredArgsConstructor
public class CollectionService {

    public static final int MAX_PAGE_SIZE = 100;
    static final String RESOURCE = "Collectie-item";

    /** Toegestane sorteervelden (API-naam → entity-pad); bij gelijke waarde op artiest en titel. */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "added", "addedAt",
            "artist", "release.artist",
            "title", "release.title",
            "year", "release.releaseYear");

    private final CollectionItemRepository collectionItemRepository;
    private final ReleaseRepository releaseRepository;
    private final CatalogService catalogService;
    private final UserService userService;
    private final CollectionItemMapper collectionItemMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * @param sort {@code veld[,asc|desc]} met veld {@code added}, {@code artist}, {@code title} of {@code year};
     *             standaard {@code added,desc} (nieuwste eerst)
     */
    @Transactional
    public CollectionPageDto list(Jwt jwt, String q, MediaType mediaType, String sort, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("page moet ≥ 0 zijn en size tussen 1 en " + MAX_PAGE_SIZE + ".");
        }
        AppUser user = userService.getOrCreate(jwt);
        Specification<CollectionItem> spec = Specification.allOf(
                CollectionItemSpecifications.ownedBy(user.getId()),
                CollectionItemSpecifications.hasMediaType(mediaType),
                CollectionItemSpecifications.matchesText(q));
        Page<CollectionItem> result = collectionItemRepository.findAll(spec, PageRequest.of(page, size, parseSort(sort)));
        return new CollectionPageDto(result.map(collectionItemMapper::toSummary).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public CollectionItemDto get(Jwt jwt, UUID id) {
        return collectionItemMapper.toDto(findOwned(jwt, id));
    }

    /**
     * Óf een kandidaat ({@code source} + {@code externalId}), óf een handmatige release. Het ophalen bij
     * een externe bron gebeurt vóór de transactie (zie {@link CatalogService#getOrImport}).
     */
    public CollectionItemDto add(Jwt jwt, AddToCollectionRequest request) {
        boolean fromSource = request.source() != null || request.externalId() != null;
        if (fromSource == (request.release() != null)) {
            throw new InvalidRequestException("Geef óf source en externalId, óf handmatige release-gegevens.");
        }
        if (fromSource && (request.source() == null || request.externalId() == null || request.externalId().isBlank())) {
            throw new InvalidRequestException("source en externalId zijn allebei nodig.");
        }
        UUID releaseId = fromSource ? catalogService.getOrImport(request.source(), request.externalId().strip()) : null;
        return transactionTemplate.execute(status -> {
            AppUser user = userService.getOrCreate(jwt);
            Release release = releaseId != null
                    ? releaseRepository.findById(releaseId).orElseThrow(() -> new ResourceNotFoundException("Release", releaseId))
                    : catalogService.createManual(request.release());
            CollectionItem item = new CollectionItem(user, release);
            collectionItemMapper.applyFields(request.fields() == null ? CollectionItemFields.EMPTY : request.fields(), item);
            return collectionItemMapper.toDto(collectionItemRepository.saveAndFlush(item));
        });
    }

    @Transactional
    public CollectionItemDto update(Jwt jwt, UUID id, CollectionItemFields fields) {
        CollectionItem item = findOwned(jwt, id);
        collectionItemMapper.applyFields(fields, item);
        return collectionItemMapper.toDto(collectionItemRepository.saveAndFlush(item));
    }

    /** Een handmatige release die nergens anders meer in staat, verdwijnt mee uit de catalogus. */
    @Transactional
    public void delete(Jwt jwt, UUID id) {
        CollectionItem item = findOwned(jwt, id);
        Release release = item.getRelease();
        collectionItemRepository.delete(item);
        collectionItemRepository.flush();
        if (release.getSource() == MetadataSource.MANUAL && collectionItemRepository.countByReleaseId(release.getId()) == 0) {
            releaseRepository.delete(release);
        }
    }

    CollectionItem findOwned(Jwt jwt, UUID id) {
        AppUser user = userService.getOrCreate(jwt);
        return collectionItemRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.desc("addedAt"));
        }
        String[] parts = sort.split(",", 2);
        String path = SORT_FIELDS.get(parts[0].strip().toLowerCase(Locale.ROOT));
        String direction = parts.length > 1 ? parts[1].strip().toLowerCase(Locale.ROOT) : "asc";
        if (path == null || !(direction.equals("asc") || direction.equals("desc"))) {
            throw new InvalidRequestException("Ongeldige sortering '" + sort + "': gebruik "
                    + String.join(", ", SORT_FIELDS.keySet().stream().sorted().toList()) + " met optioneel ,asc of ,desc.");
        }
        Sort primary = Sort.by(Sort.Direction.fromString(direction), path);
        return path.equals("addedAt")
                ? primary
                : primary.and(Sort.by("release.artist", "release.title", "addedAt"));
    }
}
