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
  template: `
    <h1>Statistieken</h1>

    @if (error()) {
      <p class="error">{{ error() }}</p>
    } @else if (stats(); as s) {
      @if (s.totalItems === 0) {
        <div class="empty">
          <p>Nog geen statistieken: je collectie is leeg.</p>
          <a mat-flat-button routerLink="/toevoegen"><mat-icon>add</mat-icon> Releases toevoegen</a>
        </div>
      } @else {
        <section class="tiles" aria-label="Kerncijfers">
          <div class="tile">
            <span class="tile-label">Releases</span>
            <span class="tile-value">{{ s.totalItems | number: '1.0-0' : 'nl' }}</span>
          </div>
          <div class="tile">
            <span class="tile-label">Aankoopwaarde</span>
            <span class="tile-value">{{
              s.totalValue | currency: 'EUR' : 'symbol' : '1.0-0' : 'nl'
            }}</span>
            <span class="tile-hint">
              {{ s.itemsWithPrice }} van {{ s.totalItems }} met aankoopprijs
            </span>
          </div>
          @if (s.itemsWithPrice > 0) {
            <div class="tile">
              <span class="tile-label">Gemiddelde prijs</span>
              <span class="tile-value">
                {{ s.totalValue / s.itemsWithPrice | currency: 'EUR' : 'symbol' : '1.2-2' : 'nl' }}
              </span>
            </div>
          }
        </section>

        <div class="charts">
          <section>
            <h2>Per type</h2>
            <app-bar-list [rows]="byType()" label="Aantal releases per type" />
          </section>
          <section>
            <h2>Per decennium</h2>
            <app-bar-list [rows]="byDecade()" label="Aantal releases per decennium" />
          </section>
          <section>
            <h2>Meest verzamelde artiesten</h2>
            <app-bar-list [rows]="topArtists()" label="Top-artiesten op aantal releases" />
          </section>
          <section>
            <h2>Staat van plaat/disc</h2>
            <app-bar-list [rows]="byCondition()" label="Aantal releases per staat" />
          </section>
        </div>
      }
    } @else {
      <mat-progress-bar mode="indeterminate" />
    }
  `,
  styles: `
    .tiles {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
      margin-bottom: 32px;
    }
    .tile {
      display: grid;
      gap: 4px;
      padding: 16px;
      border-radius: 12px;
      background: var(--mat-sys-surface-container-low);
    }
    .tile-label,
    .tile-hint {
      color: var(--mat-sys-on-surface-variant);
    }
    .tile-hint {
      font-size: 0.85rem;
    }
    .tile-value {
      font-size: 2rem;
      font-weight: 600;
    }
    .charts {
      display: grid;
      gap: 32px 48px;
    }
    @media (min-width: 960px) {
      .charts {
        grid-template-columns: 1fr 1fr;
      }
    }
    h2 {
      font-size: 1.1rem;
      margin: 0 0 12px;
    }
    .empty {
      display: grid;
      justify-items: start;
      gap: 8px;
    }
    .error {
      color: var(--mat-sys-error);
    }
  `,
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
