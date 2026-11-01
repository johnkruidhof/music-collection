import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { CollectionApiService } from '../../core/api/collection-api.service';
import { CollectionStats } from '../../core/models/stats';
import { BarList, BarRow } from '../../shared/bar-list';
import { errorMessage } from '../../shared/http-error';
import { CONDITION_LABELS, MEDIA_TYPE_LABELS } from '../../shared/labels';

/** Statistieken (F-20): kerncijfers als tegels, verdelingen als staafdiagrammen. */
@Component({
  selector: 'app-stats-page',
  imports: [
    BarList,
    CurrencyPipe,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './stats-page.html',
  styleUrl: './stats-page.scss',
})
export class StatsPage {
  private readonly collectionApi = inject(CollectionApiService);

  readonly stats = signal<CollectionStats | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly byType = computed<BarRow[]>(() =>
    (this.stats()?.byMediaType ?? []).map((r) => ({
      label: MEDIA_TYPE_LABELS[r.mediaType],
      value: r.count,
    })),
  );
  protected readonly byDecade = computed<BarRow[]>(() =>
    (this.stats()?.byDecade ?? []).map((r) => ({
      label: r.decade === null ? 'Jaar onbekend' : `${r.decade}–${r.decade + 9}`,
      value: r.count,
    })),
  );
  protected readonly topArtists = computed<BarRow[]>(() =>
    (this.stats()?.topArtists ?? []).map((r) => ({ label: r.artist, value: r.count })),
  );
  protected readonly byGenre = computed<BarRow[]>(() =>
    (this.stats()?.byGenre ?? []).map((r) => ({
      label: r.genre ?? 'Zonder genre',
      value: r.count,
    })),
  );
  protected readonly byCondition = computed<BarRow[]>(() =>
    (this.stats()?.byCondition ?? []).map((r) => ({
      label: r.condition ? CONDITION_LABELS[r.condition] : 'Niet ingevuld',
      value: r.count,
    })),
  );

  constructor() {
    this.collectionApi.stats().subscribe({
      next: (stats) => this.stats.set(stats),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }
}
