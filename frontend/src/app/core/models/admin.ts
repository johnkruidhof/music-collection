/** Spiegelt nl.johnkruidhof.musiccollection.dto.admin. */
import { MediaType, MetadataSource } from './release';

export interface AdminRelease {
  id: string;
  artist: string;
  title: string;
  releaseYear: number | null;
  mediaType: MediaType;
  formatDetails: string | null;
  label: string | null;
  catalogNumber: string | null;
  country: string | null;
  barcode: string | null;
  source: MetadataSource;
  discogsId: string | null;
  musicbrainzId: string | null;
  coverThumbnailUrl: string | null;
  collectionCount: number;
  wishlistCount: number;
  createdAt: string;
}

export interface AdminReleasePage {
  content: AdminRelease[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  /** Releases zonder collectie-items en wensen, in de hele catalogus. */
  unusedCount: number;
}
