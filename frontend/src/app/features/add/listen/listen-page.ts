import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CollectionApiService } from '../../../core/api/collection-api.service';
import { RecognitionApiService } from '../../../core/api/recognition-api.service';
import { RecognitionResult } from '../../../core/models/recognition';
import { ReleaseCandidate } from '../../../core/models/release';
import {
  AudioRecorderService,
  microphoneErrorMessage,
} from '../../../shared/audio-recorder.service';
import { CandidateCard } from '../../../shared/candidate-card';
import { Cover } from '../../../shared/cover';
import { errorMessage } from '../../../shared/http-error';
import { MediaTypePipe } from '../../../shared/label.pipes';
import { WishlistActions } from '../../../shared/wishlist-actions';

export const RECORDING_MS = 10_000;

export type ListenState =
  | { kind: 'idle' }
  | { kind: 'recording'; startedAt: number }
  | { kind: 'recognizing' }
  | { kind: 'result'; result: RecognitionResult }
  | { kind: 'error'; message: string };

/**
 * Luisteren (F-31): ~10 s opnemen terwijl de plaat speelt, herkennen via de backend (AudD) en dan
 * toevoegen, op de verlanglijst zetten of zien dat je hem al hebt.
 */
@Component({
  selector: 'app-listen-page',
  providers: [WishlistActions],
  imports: [
    CandidateCard,
    Cover,
    MediaTypePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  template: `
    @if (enabled() === false) {
      <p class="notice">
        <mat-icon>info</mat-icon>
        Luisteren is nog niet ingesteld. Zet een AudD-sleutel (<code>AUDD_API_TOKEN</code>) in
        <code>.env</code> en herstart de backend.
      </p>
    } @else if (!recorder.supported()) {
      <p class="notice">
        <mat-icon>mic_off</mat-icon>
        Opnemen kan niet in deze browser, of de pagina is niet via HTTPS geopend.
      </p>
    } @else {
      <section class="recorder">
        <p class="hint">
          Laat de plaat spelen en houd je telefoon of laptop in de buurt van de speaker.
        </p>
        @switch (state().kind) {
          @case ('recording') {
            <div class="meter" aria-hidden="true">
              <span [style.width.%]="level() * 100"></span>
            </div>
            <mat-progress-bar mode="determinate" [value]="progress()" />
            <p>Luisteren… nog {{ secondsLeft() }} s</p>
          }
          @case ('recognizing') {
            <mat-progress-bar mode="indeterminate" />
            <p>Herkennen…</p>
          }
          @default {
            <button mat-flat-button type="button" class="listen" (click)="listen()">
              <mat-icon>hearing</mat-icon>
              {{ state().kind === 'idle' ? 'Luisteren' : 'Opnieuw luisteren' }}
            </button>
          }
        }
      </section>

      @if (errorText()) {
        <p class="error">{{ errorText() }}</p>
      }

      @if (result(); as r) {
        @if (r.track; as track) {
          <section class="track" aria-live="polite">
            <mat-icon class="track-icon">music_note</mat-icon>
            <div>
              <div class="track-title">{{ track.title }}</div>
              <div class="track-meta">
                {{ track.artist }}{{ track.album ? ' · ' + track.album : ''
                }}{{ track.releaseYear ? ' (' + track.releaseYear + ')' : '' }}
              </div>
              @if (track.songLink) {
                <a mat-button [href]="track.songLink" target="_blank" rel="noopener noreferrer">
                  <mat-icon>open_in_new</mat-icon> Beluisteren
                </a>
              }
            </div>
          </section>

          @if (r.inCollection.length > 0) {
            <section>
              <h2><mat-icon>library_add_check</mat-icon> Staat al in je collectie</h2>
              <ul class="owned">
                @for (item of r.inCollection; track item.id) {
                  <li>
                    <a [routerLink]="['/collectie', item.id]">
                      <app-cover class="thumb" [url]="item.coverThumbnailUrl" [alt]="item.title" />
                      <span>
                        <strong>{{ item.title }}</strong
                        ><br />
                        <span class="meta">
                          {{ item.artist }} · {{ item.mediaType | mediaType
                          }}{{ item.storageLocation ? ' · ' + item.storageLocation : '' }}
                        </span>
                      </span>
                    </a>
                  </li>
                }
              </ul>
            </section>
          }

          <section>
            <h2>Releases met dit nummer</h2>
            @if (r.candidates.length === 0) {
              <p class="hint">
                Geen releases gevonden. Zoek zelf op
                <a
                  [routerLink]="['../zoeken']"
                  [queryParams]="{ artist: track.artist, title: track.album ?? track.title }"
                  >artiest en titel</a
                >.
              </p>
            }
            <div class="candidates">
              @for (candidate of r.candidates; track candidate.source + candidate.externalId) {
                <app-candidate-card
                  [candidate]="candidate"
                  [busy]="adding() === candidate"
                  [added]="added().has(candidate)"
                  [wishable]="true"
                  [wishBusy]="wishlist.isBusy(candidate)"
                  [wishedLocally]="wishlist.isWished(candidate)"
                  (wish)="wishlist.add($event)"
                  (add)="add($event)"
                />
              }
            </div>
          </section>
        } @else {
          <p class="notice">
            <mat-icon>help_outline</mat-icon>
            Niet herkend. Probeer het opnieuw tijdens een duidelijk stuk van het nummer, dichter bij
            de speaker.
          </p>
        }
      }
    }
  `,
  styles: `
    .recorder {
      display: grid;
      gap: 12px;
      max-width: 520px;
      margin-bottom: 24px;
    }
    .listen {
      justify-self: start;
      height: 56px;
      padding: 0 28px;
      font-size: 1.1rem;
    }
    .meter {
      height: 8px;
      border-radius: 4px;
      background: var(--mat-sys-surface-container-high);
      overflow: hidden;
    }
    .meter span {
      display: block;
      height: 100%;
      background: var(--mat-sys-primary);
      transition: width 100ms linear;
    }
    .notice {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px;
      border-radius: 8px;
      background: var(--mat-sys-secondary-container);
      color: var(--mat-sys-on-secondary-container);
    }
    .track {
      display: flex;
      gap: 16px;
      align-items: flex-start;
      padding: 16px;
      border-radius: 12px;
      background: var(--mat-sys-primary-container);
      color: var(--mat-sys-on-primary-container);
      margin-bottom: 24px;
    }
    .track-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
    }
    .track-title {
      font-size: 1.4rem;
      font-weight: 500;
    }
    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 1.15rem;
    }
    .owned {
      list-style: none;
      margin: 0 0 24px;
      padding: 0;
      display: grid;
      gap: 8px;
    }
    .owned a {
      display: flex;
      gap: 12px;
      align-items: center;
      color: inherit;
      text-decoration: none;
    }
    .thumb {
      width: 56px;
      flex: none;
    }
    .candidates {
      display: grid;
      gap: 12px;
    }
    .hint,
    .meta {
      color: var(--mat-sys-on-surface-variant);
    }
    .error {
      color: var(--mat-sys-error);
    }
  `,
})
export class ListenPage {
  protected readonly recorder = inject(AudioRecorderService);
  private readonly recognitionApi = inject(RecognitionApiService);
  private readonly collectionApi = inject(CollectionApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  protected readonly wishlist = inject(WishlistActions);

  /** null = nog onbekend */
  readonly enabled = signal<boolean | null>(null);
  readonly state = signal<ListenState>({ kind: 'idle' });
  protected readonly level = signal(0);
  private readonly now = signal(Date.now());
  readonly adding = signal<ReleaseCandidate | null>(null);
  readonly added = signal<ReadonlySet<ReleaseCandidate>>(new Set());

  protected readonly result = computed(() => {
    const s = this.state();
    return s.kind === 'result' ? s.result : null;
  });
  protected readonly errorText = computed(() => {
    const s = this.state();
    return s.kind === 'error' ? s.message : null;
  });
  protected readonly progress = computed(() => {
    const s = this.state();
    return s.kind === 'recording'
      ? Math.min(100, ((this.now() - s.startedAt) / RECORDING_MS) * 100)
      : 0;
  });
  protected readonly secondsLeft = computed(() =>
    Math.max(0, Math.ceil(((100 - this.progress()) / 100) * 10)),
  );

  constructor() {
    this.recognitionApi.enabled().subscribe({
      next: (enabled) => this.enabled.set(enabled),
      error: () => this.enabled.set(true),
    });
  }

  async listen(): Promise<void> {
    if (this.state().kind === 'recording' || this.state().kind === 'recognizing') {
      return;
    }
    this.state.set({ kind: 'recording', startedAt: Date.now() });
    this.added.set(new Set());
    const ticker = setInterval(() => this.now.set(Date.now()), 200);
    let recording;
    try {
      recording = await this.recorder.record(RECORDING_MS, (level) => this.level.set(level));
    } catch (err) {
      this.state.set({ kind: 'error', message: microphoneErrorMessage(err) });
      return;
    } finally {
      clearInterval(ticker);
      this.level.set(0);
    }
    this.state.set({ kind: 'recognizing' });
    this.recognitionApi.recognize(recording.blob, recording.fileName).subscribe({
      next: (result) => this.state.set({ kind: 'result', result }),
      error: (err) => this.state.set({ kind: 'error', message: errorMessage(err) }),
    });
  }

  add(candidate: ReleaseCandidate): void {
    if (this.adding()) {
      return;
    }
    this.adding.set(candidate);
    this.collectionApi.addCandidate(candidate).subscribe({
      next: (item) => {
        this.adding.set(null);
        this.added.update((set) => new Set(set).add(candidate));
        this.snackBar
          .open(`Toegevoegd: ${item.release.artist} – ${item.release.title}`, 'Bekijken', {
            duration: 4000,
          })
          .onAction()
          .subscribe(() => void this.router.navigate(['/collectie', item.id]));
      },
      error: (err) => {
        this.adding.set(null);
        this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 });
      },
    });
  }
}
