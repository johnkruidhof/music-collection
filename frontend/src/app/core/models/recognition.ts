/** Spiegelt nl.johnkruidhof.musiccollection.dto.recognition. */
import { CollectionItemSummary } from './collection';
import { ReleaseCandidate } from './release';

export interface RecognizedTrack {
  artist: string;
  title: string;
  album: string | null;
  releaseYear: number | null;
  label: string | null;
  /** Pagina met links naar streamingdiensten. */
  songLink: string | null;
}

/** Welke track van een eigen item het herkende nummer is (F-32). */
export interface TrackMatch {
  itemId: string;
  discNumber: number;
  position: string | null;
  title: string;
}

export interface RecognitionResult {
  /** null = niets herkend */
  track: RecognizedTrack | null;
  /** Eigen items; items waarop het nummer echt als track staat eerst. */
  inCollection: CollectionItemSummary[];
  trackMatches: TrackMatch[];
  /** Leeg bij scope 'collection'. */
  candidates: ReleaseCandidate[];
}

/** 'collection' = alleen in de eigen collectie zoeken ("Welke van mijn platen is dit?"). */
export type RecognitionScope = 'all' | 'collection';
