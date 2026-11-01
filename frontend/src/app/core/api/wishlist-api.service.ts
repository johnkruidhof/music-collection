import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CollectionItem, CollectionItemFields } from '../models/collection';
import { ReleaseCandidate } from '../models/release';
import { WishlistItem } from '../models/wishlist';

/** /api/wishlist — de verlanglijst (F-24). */
@Injectable({ providedIn: 'root' })
export class WishlistApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<WishlistItem[]> {
    return this.http.get<WishlistItem[]>('/api/wishlist');
  }

  /** Idempotent: staat de release er al op, dan komt de bestaande wens terug. */
  addCandidate(candidate: ReleaseCandidate, notes: string | null = null): Observable<WishlistItem> {
    return this.http.post<WishlistItem>('/api/wishlist', {
      source: candidate.source,
      externalId: candidate.externalId,
      notes,
    });
  }

  setGenre(id: string, genreId: string | null): Observable<WishlistItem> {
    return this.http.put<WishlistItem>(`/api/wishlist/${id}/genre`, { genreId });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/wishlist/${id}`);
  }

  /** Gekocht: wordt een collectie-item. */
  acquire(id: string, fields: CollectionItemFields | null = null): Observable<CollectionItem> {
    return this.http.post<CollectionItem>(`/api/wishlist/${id}/acquire`, fields);
  }
}
