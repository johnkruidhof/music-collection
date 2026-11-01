/** Spiegelt nl.johnkruidhof.musiccollection.dto.genre (F-28). */
export interface Genre {
  id: string;
  name: string;
  /** null = hoofdgenre */
  parentId: string | null;
  /** Collectie-items met precies dit genre. */
  itemCount: number;
  subgenres: Genre[];
}

export interface GenreImportResult {
  genresCreated: number;
  subgenresCreated: number;
  artistsLinked: number;
  itemsAssigned: number;
  wishesAssigned: number;
}

/** Eén keuze in een genre-keuzelijst; subgenres springen in. */
export interface GenreOption {
  id: string;
  name: string;
  /** "Hardrock - Metal › Doom Metal" */
  label: string;
  isSubgenre: boolean;
}
