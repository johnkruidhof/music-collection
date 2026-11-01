import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Genre, GenreImportResult } from '../models/genre';

/** /api/genres — eigen genres (F-28/F-29). */
@Injectable({ providedIn: 'root' })
export class GenreApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<Genre[]> {
    return this.http.get<Genre[]>('/api/genres');
  }

  create(name: string, parentId: string | null = null): Observable<Genre> {
    return this.http.post<Genre>('/api/genres', { name, parentId });
  }

  rename(id: string, name: string): Observable<Genre> {
    return this.http.put<Genre>(`/api/genres/${id}`, { name });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/genres/${id}`);
  }

  /** Boomstructuur van een muziekmap (tekstbestand). */
  importTree(file: File): Observable<GenreImportResult> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<GenreImportResult>('/api/genres/import', form);
  }
}
