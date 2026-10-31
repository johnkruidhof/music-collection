import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { UserApiService } from '../api/user-api.service';
import { User } from '../models/user';

/** De ingelogde gebruiker volgens de backend ({@code /api/me}), o.a. voor de rol {@code admin}. */
@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private readonly userApi = inject(UserApiService);

  readonly user = signal<User | null>(null);
  readonly isAdmin = computed(() => this.user()?.roles.includes('admin') ?? false);

  /** Laadt de gebruiker één keer; fouten (bv. offline) leveren {@code null}. */
  load(): Observable<User | null> {
    const current = this.user();
    if (current) {
      return of(current);
    }
    return this.userApi.me().pipe(
      tap((user) => this.user.set(user)),
      catchError(() => of(null)),
    );
  }

  hasRole(role: string): Observable<boolean> {
    return this.load().pipe(map((user) => user?.roles.includes(role) ?? false));
  }

  clear(): void {
    this.user.set(null);
  }
}
