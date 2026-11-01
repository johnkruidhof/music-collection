import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { GenreApiService } from '../core/api/genre-api.service';
import { Genre, GenreOption } from '../core/models/genre';

/** Ondertekening tussen genre en subgenre in labels. */
export const GENRE_SEPARATOR = ' › ';

/** Volledige naam: "Hardrock - Metal › Doom Metal", of alleen het genre. */
export function genreLabel(genreName: string | null, subgenreName: string | null): string {
  if (!genreName) {
    return 'Zonder genre';
  }
  return subgenreName ? genreName + GENRE_SEPARATOR + subgenreName : genreName;
}

/**
 * De genres van de gebruiker, één keer geladen en gedeeld door alle schermen (F-29). Na wijzigen
 * (beheerpagina, import) {@link reload} aanroepen.
 */
@Injectable({ providedIn: 'root' })
export class GenreStore {
  private readonly genreApi = inject(GenreApiService);

  readonly genres = signal<Genre[]>([]);
  readonly loaded = signal(false);

  /** Platte lijst voor keuzelijsten: elk hoofdgenre gevolgd door zijn subgenres. */
  readonly options = computed<GenreOption[]>(() =>
    this.genres().flatMap((genre) => [
      { id: genre.id, name: genre.name, label: genre.name, isSubgenre: false },
      ...genre.subgenres.map((sub) => ({
        id: sub.id,
        name: sub.name,
        label: genre.name + GENRE_SEPARATOR + sub.name,
        isSubgenre: true,
      })),
    ]),
  );

  /** Laadt de genres als dat nog niet gebeurd is. */
  ensureLoaded(): void {
    if (!this.loaded()) {
      this.reload().subscribe({ error: () => this.loaded.set(false) });
    }
  }

  reload(): Observable<Genre[]> {
    this.loaded.set(true);
    return this.genreApi.list().pipe(tap((genres) => this.genres.set(genres)));
  }

  /**
   * Hoort een item met genre {@code itemGenreId} bij filter {@code filter}? Filter = genre-id (inclusief
   * zijn subgenres), 'none' (zonder genre) of null (alles).
   */
  matches(filter: string | null, itemGenreId: string | null): boolean {
    if (!filter) {
      return true;
    }
    if (filter === 'none') {
      return !itemGenreId;
    }
    if (filter === itemGenreId) {
      return true;
    }
    return !!this.genres()
      .find((g) => g.id === filter)
      ?.subgenres.some((s) => s.id === itemGenreId);
  }

  labelFor(id: string | null | undefined): string {
    return this.options().find((o) => o.id === id)?.label ?? 'Zonder genre';
  }
}
