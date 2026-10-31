/** Spiegelt nl.johnkruidhof.musiccollection.dto.collection. */
import { ManualRelease, MediaCondition, MediaType, MetadataSource, Release } from './release';

export interface CollectionItemFields {
  conditionMedia: MediaCondition | null;
  conditionSleeve: MediaCondition | null;
  /** ISO-datum (yyyy-mm-dd). */
  purchaseDate: string | null;
  purchasePrice: number | null;
  purchaseLocation: string | null;
  storageLocation: string | null;
  notes: string | null;
}

export interface CollectionItem extends CollectionItemFields {
  id: string;
  release: Release;
  addedAt: string;
  updatedAt: string;
}

export interface CollectionItemSummary {
  id: string;
  releaseId: string;
  artist: string;
  title: string;
  releaseYear: number | null;
  mediaType: MediaType;
  formatDetails: string | null;
  coverThumbnailUrl: string | null;
  conditionMedia: MediaCondition | null;
  storageLocation: string | null;
  addedAt: string;
}

export interface CollectionPage {
  content: CollectionItemSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Óf `source` + `externalId`, óf `release`. */
export interface AddToCollectionRequest {
  source?: MetadataSource;
  externalId?: string;
  release?: ManualRelease;
  fields?: CollectionItemFields | null;
}

export type CollectionSortField = 'added' | 'artist' | 'title' | 'year';

export interface CollectionQuery {
  q?: string | null;
  mediaType?: MediaType | null;
  /** bv. "artist,asc" of "added,desc" */
  sort?: string | null;
  page?: number;
  size?: number;
}
