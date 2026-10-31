/** Spiegelt de enums en DTO's uit nl.johnkruidhof.musiccollection.{entity,dto.release,dto.lookup}. */

export type MediaType = 'VINYL' | 'CD' | 'DVD' | 'BLU_RAY' | 'CASSETTE' | 'OTHER';
export type MetadataSource = 'DISCOGS' | 'MUSICBRAINZ' | 'MANUAL';
export type MediaCondition =
  'MINT' | 'NEAR_MINT' | 'VERY_GOOD_PLUS' | 'VERY_GOOD' | 'GOOD' | 'POOR';

export const MEDIA_TYPES: readonly MediaType[] = [
  'VINYL',
  'CD',
  'DVD',
  'BLU_RAY',
  'CASSETTE',
  'OTHER',
];
export const MEDIA_CONDITIONS: readonly MediaCondition[] = [
  'MINT',
  'NEAR_MINT',
  'VERY_GOOD_PLUS',
  'VERY_GOOD',
  'GOOD',
  'POOR',
];

/** Zoekresultaat waaruit de gebruiker kiest (nog zonder tracks). */
export interface ReleaseCandidate {
  source: MetadataSource;
  externalId: string;
  artist: string | null;
  title: string | null;
  releaseYear: number | null;
  mediaType: MediaType;
  formatDetails: string | null;
  label: string | null;
  catalogNumber: string | null;
  country: string | null;
  barcode: string | null;
  coverThumbnailUrl: string | null;
}

export interface Track {
  discNumber: number;
  position: string | null;
  sortOrder: number;
  title: string;
  durationSeconds: number | null;
  artist: string | null;
}

export interface Release {
  /** null zolang de release nog niet in de catalogus staat. */
  id: string | null;
  barcode: string | null;
  artist: string;
  title: string;
  releaseYear: number | null;
  mediaType: MediaType;
  formatDetails: string | null;
  label: string | null;
  catalogNumber: string | null;
  country: string | null;
  coverImageUrl: string | null;
  coverThumbnailUrl: string | null;
  source: MetadataSource;
  discogsId: string | null;
  musicbrainzId: string | null;
  tracks: Track[];
}

export interface ReleaseSearchCriteria {
  artist?: string | null;
  title?: string | null;
  year?: number | null;
  mediaType?: MediaType | null;
}

/** Handmatig ingevoerde release (POST /api/collection met `release`). */
export interface ManualRelease {
  barcode: string | null;
  artist: string;
  title: string;
  releaseYear: number | null;
  mediaType: MediaType;
  formatDetails: string | null;
  label: string | null;
  catalogNumber: string | null;
  country: string | null;
  tracks: Track[];
}
