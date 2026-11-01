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
  templateUrl: './candidate-card.html',
  styleUrl: './candidate-card.scss',
})
export class CandidateCard {
  readonly candidate = input.required<ReleaseCandidate>();
  readonly busy = input(false);
  readonly added = input(false);
  readonly add = output<ReleaseCandidate>();
  /** Toon de knop "Op verlanglijst" (F-24). */
  readonly wishable = input(false);
  readonly wishBusy = input(false);
  /** Lokaal op de verlanglijst gezet; anders telt {@code candidate.onWishlist}. */
  readonly wishedLocally = input(false);
  readonly wish = output<ReleaseCandidate>();

  protected readonly wished = computed(() => this.wishedLocally() || !!this.candidate().onWishlist);

  protected readonly owned = computed(() => this.candidate().ownedCount ?? 0);
}
