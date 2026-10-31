import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { User } from '../models/user';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly http = inject(HttpClient);

  /** Haalt de ingelogde gebruiker op; de backend maakt het account aan bij de eerste aanroep. */
  me(): Observable<User> {
    return this.http.get<User>('/api/me');
  }
}
