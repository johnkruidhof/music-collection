import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminRelease, AdminReleasePage } from '../models/admin';
import { MetadataSource } from '../models/release';

/** /api/admin/releases — catalogusbeheer (rol admin, F-27). */
@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);

  search(
    q: string | null,
    source: MetadataSource | null,
    page = 0,
    size = 25,
  ): Observable<AdminReleasePage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) {
      params = params.set('q', q);
    }
    if (source) {
      params = params.set('source', source);
    }
    return this.http.get<AdminReleasePage>('/api/admin/releases', { params });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/releases/${id}`);
  }

  deleteUnused(): Observable<{ deleted: number }> {
    return this.http.delete<{ deleted: number }>('/api/admin/releases/unused');
  }

  /** {@code sourceId} gaat op in {@code targetId}. */
  merge(sourceId: string, targetId: string): Observable<AdminRelease> {
    return this.http.post<AdminRelease>(`/api/admin/releases/${sourceId}/merge`, { targetId });
  }
}
