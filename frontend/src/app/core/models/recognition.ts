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

export interface RecognitionResult {
  /** null = niets herkend */
  track: RecognizedTrack | null;
  inCollection: CollectionItemSummary[];
  candidates: ReleaseCandidate[];
}
