import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  MetadataSource,
  Release,
  ReleaseCandidate,
  ReleaseSearchCriteria,
} from '../models/release';

/** /api/lookup — kandidaten uit de catalogus, Discogs en MusicBrainz. */
@Injectable({ providedIn: 'root' })
export class LookupApiService {
  private readonly http = inject(HttpClient);

  byBarcode(barcode: string): Observable<ReleaseCandidate[]> {
    return this.http.get<ReleaseCandidate[]>(`/api/lookup/barcode/${encodeURIComponent(barcode)}`);
  }

  search(criteria: ReleaseSearchCriteria): Observable<ReleaseCandidate[]> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(criteria)) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<ReleaseCandidate[]>('/api/lookup/search', { params });
  }

  details(source: MetadataSource, externalId: string): Observable<Release> {
    return this.http.get<Release>(
      `/api/lookup/${source.toLowerCase()}/${encodeURIComponent(externalId)}`,
    );
  }
}
