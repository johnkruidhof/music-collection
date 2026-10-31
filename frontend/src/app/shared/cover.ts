import { Component, computed, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Cover met placeholder: zonder URL, of als de afbeelding niet laadt (bv. een MusicBrainz-
 * thumbnail zonder cover geeft 404), verschijnt een icoon.
 */
@Component({
  selector: 'app-cover',
  imports: [MatIconModule],
  template: `
    @if (showImage()) {
      <img
        [src]="url()"
        [alt]="alt()"
        loading="lazy"
        referrerpolicy="no-referrer"
        (error)="failedUrl.set(url())"
      />
    } @else {
      <div class="placeholder" role="img" [attr.aria-label]="alt() || 'Geen cover'">
        <mat-icon>album</mat-icon>
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
      aspect-ratio: 1;
      overflow: hidden;
      border-radius: 4px;
      background: var(--mat-sys-surface-container-high);
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .placeholder {
      width: 100%;
      height: 100%;
      display: grid;
      place-items: center;
      color: var(--mat-sys-on-surface-variant);
    }
    .placeholder mat-icon {
      width: 40%;
      height: 40%;
      font-size: min(64px, 40cqw);
    }
  `,
})
export class Cover {
  readonly url = input<string | null | undefined>(null);
  readonly alt = input('');

  protected readonly failedUrl = signal<string | null | undefined>(undefined);
  protected readonly showImage = computed(() => !!this.url() && this.failedUrl() !== this.url());
}
