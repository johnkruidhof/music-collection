import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AddToCollectionRequest,
  CollectionItem,
  CollectionItemFields,
  CollectionPage,
  CollectionQuery,
} from '../models/collection';
import { ManualRelease, ReleaseCandidate } from '../models/release';

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
