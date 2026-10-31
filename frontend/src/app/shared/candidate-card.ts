import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReleaseCandidate } from '../core/models/release';
import { Cover } from './cover';
import { MediaTypePipe, SourcePipe } from './label.pipes';

/** Eén kandidaat (persing) met cover, gegevens en een "Toevoegen"-knop. */
@Component({
  selector: 'app-candidate-card',
  imports: [
    Cover,
    MediaTypePipe,
    SourcePipe,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <app-cover
      class="cover"
      [url]="candidate().coverThumbnailUrl"
      [alt]="candidate().title ?? ''"
    />
    <div class="info">
      <div class="artist">{{ candidate().artist ?? 'Onbekende artiest' }}</div>
      <div class="title">{{ candidate().title ?? 'Zonder titel' }}</div>
      <div class="meta">
        <span class="type">{{ candidate().mediaType | mediaType }}</span>
        @if (candidate().releaseYear) {
          <span>{{ candidate().releaseYear }}</span>
        }
        @if (candidate().country) {
          <span>{{ candidate().country }}</span>
        }
      </div>
      @if (candidate().formatDetails) {
        <div class="details">{{ candidate().formatDetails }}</div>
      }
      @if (candidate().label || candidate().catalogNumber) {
        <div class="details">
          {{ candidate().label }}{{ candidate().label && candidate().catalogNumber ? ' – ' : ''
          }}{{ candidate().catalogNumber }}
        </div>
      }
      <div class="source">{{ candidate().source | source }}</div>
      @if (owned() > 0) {
        <div class="owned">
          <mat-icon inline>library_add_check</mat-icon>
          Al in je collectie{{ owned() > 1 ? ' (' + owned() + '×)' : '' }}
        </div>
      }
    </div>
    <div class="actions">
      @if (added()) {
        <span class="added"><mat-icon>check_circle</mat-icon> Toegevoegd</span>
      } @else {
        <button mat-flat-button type="button" [disabled]="busy()" (click)="add.emit(candidate())">
          @if (busy()) {
            <mat-progress-spinner diameter="18" mode="indeterminate" />
          } @else {
            <mat-icon>add</mat-icon>
          }
          {{ owned() > 0 ? 'Nog een exemplaar' : 'Toevoegen' }}
        </button>
      }
      <ng-content />
    </div>
  `,
  styles: `
    :host {
      display: grid;
      grid-template-columns: 96px 1fr;
      grid-template-areas: 'cover info' 'actions actions';
      gap: 12px 16px;
      padding: 12px;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 12px;
      background: var(--mat-sys-surface-container-low);
    }
    .cover {
      grid-area: cover;
      width: 96px;
    }
    .info {
      grid-area: info;
      min-width: 0;
    }
    .artist {
      color: var(--mat-sys-on-surface-variant);
    }
    .title {
      font-size: 1.15rem;
      font-weight: 500;
      overflow-wrap: anywhere;
    }
    .meta {
      display: flex;
      flex-wrap: wrap;
      gap: 4px 12px;
      margin-top: 4px;
    }
    .type {
      font-weight: 500;
      color: var(--mat-sys-primary);
    }
    .details,
    .source {
      font-size: 0.85rem;
      color: var(--mat-sys-on-surface-variant);
    }
    .source {
      margin-top: 4px;
    }
    .owned {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      margin-top: 6px;
      padding: 2px 8px;
      border-radius: 8px;
      font-size: 0.85rem;
      background: var(--mat-sys-tertiary-container);
      color: var(--mat-sys-on-tertiary-container);
    }
    .actions {
      grid-area: actions;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      align-items: center;
    }
    .added {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: var(--mat-sys-primary);
      font-weight: 500;
    }
    @media (min-width: 600px) {
      :host {
        grid-template-columns: 120px 1fr auto;
        grid-template-areas: 'cover info actions';
      }
      .cover {
        width: 120px;
      }
      .actions {
        flex-direction: column;
        align-items: stretch;
      }
    }
  `,
})
export class CandidateCard {
  readonly candidate = input.required<ReleaseCandidate>();
  readonly busy = input(false);
  readonly added = input(false);
  readonly add = output<ReleaseCandidate>();

  protected readonly owned = computed(() => this.candidate().ownedCount ?? 0);
}
