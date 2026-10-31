import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import {
  AddToCollectionRequest,
  CollectionItem,
  CollectionItemFields,
  CollectionPage,
  CollectionQuery,
} from '../models/collection';
import { ManualRelease, ReleaseCandidate } from '../models/release';
import { CollectionStats } from '../models/stats';
import { ImportJob } from '../models/import-job';

/** /api/collection — de eigen collectie. */
@Injectable({ providedIn: 'root' })
export class CollectionApiService {
  private readonly http = inject(HttpClient);

  list(query: CollectionQuery): Observable<CollectionPage> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<CollectionPage>('/api/collection', { params });
  }

  /** CSV-export; de bestandsnaam komt uit Content-Disposition. */
  exportCsv(): Observable<{ blob: Blob; fileName: string }> {
    return this.http
      .get('/api/collection/export', { responseType: 'blob', observe: 'response' })
      .pipe(
        map((response) => ({
          blob: response.body ?? new Blob(),
          fileName:
            /filename="([^"]+)"/.exec(response.headers.get('Content-Disposition') ?? '')?.[1] ??
            'muziekcollectie.csv',
        })),
      );
  }

  importDiscogs(file: File): Observable<ImportJob> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<ImportJob>('/api/collection/import/discogs', form);
  }

  importStatus(id: string): Observable<ImportJob> {
    return this.http.get<ImportJob>(`/api/collection/import/${id}`);
  }

  /** De laatste import sinds de herstart van de backend, of null (204). */
  latestImport(): Observable<ImportJob | null> {
    return this.http.get<ImportJob | null>('/api/collection/import/latest');
  }

  stats(): Observable<CollectionStats> {
    return this.http.get<CollectionStats>('/api/collection/stats');
  }

  get(id: string): Observable<CollectionItem> {
    return this.http.get<CollectionItem>(`/api/collection/${id}`);
  }

  add(request: AddToCollectionRequest): Observable<CollectionItem> {
    return this.http.post<CollectionItem>('/api/collection', request);
  }

  /** Een gekozen kandidaat toevoegen; de backend haalt de details zelf op. */
  addCandidate(candidate: ReleaseCandidate): Observable<CollectionItem> {
    return this.add({ source: candidate.source, externalId: candidate.externalId });
  }

  update(id: string, fields: CollectionItemFields): Observable<CollectionItem> {
    return this.http.put<CollectionItem>(`/api/collection/${id}`, fields);
  }

  /** Alleen voor handmatig ingevoerde releases. */
  updateRelease(id: string, release: ManualRelease): Observable<CollectionItem> {
    return this.http.put<CollectionItem>(`/api/collection/${id}/release`, release);
  }

  /** Eigen cover (alleen handmatige releases); verklein eerst met {@link resizeImage}. */
  uploadCover(id: string, image: Blob): Observable<CollectionItem> {
    const form = new FormData();
    form.append('file', image, 'cover.jpg');
    return this.http.post<CollectionItem>(`/api/collection/${id}/cover`, form);
  }

  deleteCover(id: string): Observable<CollectionItem> {
    return this.http.delete<CollectionItem>(`/api/collection/${id}/cover`);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/collection/${id}`);
  }
}
