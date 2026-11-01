import { Component, computed, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Cover met placeholder: zonder URL, of als de afbeelding niet laadt (bv. een MusicBrainz-
 * thumbnail zonder cover geeft 404), verschijnt een icoon.
 */
@Component({
  selector: 'app-cover',
  imports: [MatIconModule],
  templateUrl: './cover.html',
  styleUrl: './cover.scss',
})
export class Cover {
  readonly url = input<string | null | undefined>(null);
  readonly alt = input('');

  protected readonly failedUrl = signal<string | null | undefined>(undefined);
  protected readonly showImage = computed(() => !!this.url() && this.failedUrl() !== this.url());
}
