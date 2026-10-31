/** Spiegelt nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto. */
import { MediaCondition, MediaType } from './release';

export interface CollectionStats {
  totalItems: number;
  totalValue: number;
  itemsWithPrice: number;
  byMediaType: { mediaType: MediaType; count: number }[];
  /** decade null = jaar onbekend */
  byDecade: { decade: number | null; count: number }[];
  topArtists: { artist: string; count: number }[];
  /** condition null = staat niet ingevuld */
  byCondition: { condition: MediaCondition | null; count: number }[];
}
