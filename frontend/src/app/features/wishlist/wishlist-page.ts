import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { filter, switchMap } from 'rxjs';
import { WishlistApiService } from '../../core/api/wishlist-api.service';
import { WishlistItem } from '../../core/models/wishlist';
import { ConfirmDialog, ConfirmDialogData } from '../../shared/confirm-dialog';
import { Cover } from '../../shared/cover';
import { errorMessage } from '../../shared/http-error';
import { MediaTypePipe } from '../../shared/label.pipes';

/** Verlanglijst (F-24): wensen bekijken, als gekocht naar de collectie verplaatsen of weghalen. */
@Component({
  selector: 'app-wishlist-page',
  imports: [Cover, MediaTypePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './wishlist-page.html',
  styleUrl: './wishlist-page.scss',
})
export class WishlistPage {
  private readonly wishlistApi = inject(WishlistApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly items = signal<WishlistItem[] | null>(null);
  readonly busy = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.wishlistApi.list().subscribe({
      next: (items) => this.items.set(items),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  acquire(item: WishlistItem): void {
    this.busy.set(item.id);
    this.wishlistApi.acquire(item.id).subscribe({
      next: (collectionItem) => {
        this.busy.set(null);
        this.removeLocally(item);
        this.snackBar
          .open(`In je collectie: ${item.artist} – ${item.title}`, 'Bekijken', { duration: 5000 })
          .onAction()
          .subscribe(() => void this.router.navigate(['/collectie', collectionItem.id]));
      },
      error: (err) => {
        this.busy.set(null);
        this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 });
      },
    });
  }

  remove(item: WishlistItem): void {
    this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
        data: {
          title: 'Weghalen van je verlanglijst?',
          message: `${item.artist} – ${item.title}`,
          confirmLabel: 'Weghalen',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed === true),
        switchMap(() => {
          this.busy.set(item.id);
          return this.wishlistApi.delete(item.id);
        }),
      )
      .subscribe({
        next: () => {
          this.busy.set(null);
          this.removeLocally(item);
        },
        error: (err) => {
          this.busy.set(null);
          this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 });
        },
      });
  }

  private removeLocally(item: WishlistItem): void {
    this.items.update((list) => (list ?? []).filter((i) => i.id !== item.id));
  }
}
