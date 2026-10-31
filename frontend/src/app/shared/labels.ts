import { MediaCondition, MediaType, MetadataSource } from '../core/models/release';

export const MEDIA_TYPE_LABELS: Record<MediaType, string> = {
  VINYL: 'Vinyl',
  CD: 'CD',
  DVD: 'DVD',
  BLU_RAY: 'Blu-ray',
  CASSETTE: 'Cassette',
  OTHER: 'Overig',
};

/** Goldmine-schaal. */
export const CONDITION_LABELS: Record<MediaCondition, string> = {
  MINT: 'Mint (M)',
  NEAR_MINT: 'Near Mint (NM)',
  VERY_GOOD_PLUS: 'Very Good Plus (VG+)',
  VERY_GOOD: 'Very Good (VG)',
  GOOD: 'Good (G)',
  POOR: 'Poor (P)',
};

export const SOURCE_LABELS: Record<MetadataSource, string> = {
  DISCOGS: 'Discogs',
  MUSICBRAINZ: 'MusicBrainz',
  MANUAL: 'Handmatig',
};

/** 284 → "4:44", 3723 → "1:02:03". */
export function formatDuration(seconds: number | null | undefined): string {
  if (seconds === null || seconds === undefined || seconds < 0) {
    return '';
  }
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = String(seconds % 60).padStart(2, '0');
  return h > 0 ? `${h}:${String(m).padStart(2, '0')}:${s}` : `${m}:${s}`;
}

/** "4:44" → 284, "1:02:03" → 3723, leeg → null, onleesbaar → undefined. */
export function parseDuration(text: string | null | undefined): number | null | undefined {
  const value = (text ?? '').trim();
  if (value === '') {
    return null;
  }
  const match = /^(?:(\d+):)?(\d{1,3}):([0-5]\d)$/.exec(value);
  if (!match) {
    return undefined;
  }
  return Number(match[1] ?? 0) * 3600 + Number(match[2]) * 60 + Number(match[3]);
}
