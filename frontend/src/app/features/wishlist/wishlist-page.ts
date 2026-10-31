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
  template: `
    <header class="header">
      <h1>
        Verlanglijst
        @if (items(); as list) {
          <span class="count">({{ list.length }})</span>
        }
      </h1>
      <a mat-stroked-button routerLink="/toevoegen/zoeken"><mat-icon>search</mat-icon> Zoeken</a>
    </header>

    @if (error()) {
      <p class="error">{{ error() }}</p>
    } @else if (items(); as list) {
      @if (list.length === 0) {
        <div class="empty">
          <mat-icon>bookmark_border</mat-icon>
          <p>Je verlanglijst is leeg. Zoek of scan een release en kies "Op verlanglijst".</p>
        </div>
      } @else {
        <ul class="list">
          @for (item of list; track item.id) {
            <li class="item">
              <app-cover class="cover" [url]="item.coverThumbnailUrl" [alt]="item.title" />
              <div class="info">
                <span class="artist">{{ item.artist }}</span>
                <span class="title">{{ item.title }}</span>
                <span class="meta">
                  {{ item.mediaType | mediaType
                  }}{{ item.releaseYear ? ' · ' + item.releaseYear : ''
                  }}{{ item.country ? ' · ' + item.country : '' }}
                </span>
                @if (item.formatDetails) {
                  <span class="meta">{{ item.formatDetails }}</span>
                }
                @if (item.notes) {
                  <span class="notes">{{ item.notes }}</span>
                }
              </div>
              <div class="actions">
                <button
                  mat-flat-button
                  type="button"
                  [disabled]="busy() === item.id"
                  (click)="acquire(item)"
                >
                  <mat-icon>shopping_bag</mat-icon> Gekocht
                </button>
                <button
                  mat-button
                  type="button"
                  [disabled]="busy() === item.id"
                  (click)="remove(item)"
                >
                  <mat-icon>delete</mat-icon> Weghalen
                </button>
              </div>
            </li>
          }
        </ul>
      }
    } @else {
      <mat-progress-bar mode="indeterminate" />
    }
  `,
  styles: `
    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }
    .count {
      color: var(--mat-sys-on-surface-variant);
      font-weight: 400;
    }
    .list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 12px;
    }
    .item {
      display: grid;
      grid-template-columns: 88px 1fr;
      grid-template-areas: 'cover info' 'actions actions';
      gap: 12px 16px;
      padding: 12px;
      border-radius: 12px;
      background: var(--mat-sys-surface-container-low);
    }
    @media (min-width: 600px) {
      .item {
        grid-template-columns: 88px 1fr auto;
        grid-template-areas: 'cover info actions';
        align-items: center;
      }
    }
    .cover {
      grid-area: cover;
      width: 88px;
    }
    .info {
      grid-area: info;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }
    .title {
      font-weight: 500;
      font-size: 1.1rem;
    }
    .artist,
    .meta,
    .notes {
      color: var(--mat-sys-on-surface-variant);
    }
    .meta,
    .notes {
      font-size: 0.875rem;
    }
    .notes {
      font-style: italic;
    }
    .actions {
      grid-area: actions;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    .empty {
      display: grid;
      justify-items: center;
      gap: 8px;
      padding: 48px 16px;
      text-align: center;
      color: var(--mat-sys-on-surface-variant);
    }
    .empty mat-icon {
      width: 48px;
      height: 48px;
      font-size: 48px;
    }
    .error {
      color: var(--mat-sys-error);
    }
  `,
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
