import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { RecognitionResult } from '../models/recognition';

/** /api/recognize — muziek herkennen uit een opname (F-30/F-31). */
@Injectable({ providedIn: 'root' })
export class RecognitionApiService {
  private readonly http = inject(HttpClient);

  enabled(): Observable<boolean> {
    return this.http.get<{ enabled: boolean }>('/api/recognize/status').pipe(map((s) => s.enabled));
  }

  recognize(audio: Blob, fileName: string): Observable<RecognitionResult> {
    const form = new FormData();
    form.append('file', audio, fileName);
    return this.http.post<RecognitionResult>('/api/recognize', form);
  }
}
