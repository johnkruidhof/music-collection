import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { WishlistApiService } from '../core/api/wishlist-api.service';
import { ReleaseCandidate } from '../core/models/release';
import { errorMessage } from './http-error';

const key = (c: ReleaseCandidate) => `${c.source}:${c.externalId}`;

/**
 * "Op verlanglijst" vanuit de scan- en zoekpagina: houdt bij welke kandidaten bezig zijn of al zijn
 * toegevoegd. Per pagina een eigen instantie ({@code providers: [WishlistActions]}).
 */
@Injectable()
export class WishlistActions {
  private readonly wishlistApi = inject(WishlistApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  private readonly busyKey = signal<string | null>(null);
  private readonly wishedKeys = signal<ReadonlySet<string>>(new Set());

  isBusy(candidate: ReleaseCandidate): boolean {
    return this.busyKey() === key(candidate);
  }

  isWished(candidate: ReleaseCandidate): boolean {
    return this.wishedKeys().has(key(candidate));
  }

  add(candidate: ReleaseCandidate): void {
    if (this.busyKey()) {
      return;
    }
    this.busyKey.set(key(candidate));
    this.wishlistApi.addCandidate(candidate).subscribe({
      next: (wish) => {
        this.busyKey.set(null);
        this.wishedKeys.update((set) => new Set(set).add(key(candidate)));
        this.snackBar
          .open(`Op je verlanglijst: ${wish.artist} – ${wish.title}`, 'Verlanglijst', {
            duration: 4000,
          })
          .onAction()
          .subscribe(() => void this.router.navigate(['/verlanglijst']));
      },
      error: (err) => {
        this.busyKey.set(null);
        this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 });
      },
    });
  }
}
