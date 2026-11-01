/** Spiegelt nl.johnkruidhof.musiccollection.dto.wishlist. */
import { MediaType, MetadataSource } from './release';

export interface WishlistItem {
  id: string;
  releaseId: string;
  artist: string;
  title: string;
  releaseYear: number | null;
  mediaType: MediaType;
  formatDetails: string | null;
  label: string | null;
  country: string | null;
  coverThumbnailUrl: string | null;
  source: MetadataSource;
  discogsId: string | null;
  musicbrainzId: string | null;
  notes: string | null;
  genreId: string | null;
  genreName: string | null;
  subgenreName: string | null;
  addedAt: string;
}
