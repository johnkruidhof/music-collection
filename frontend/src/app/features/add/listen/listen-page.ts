import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CollectionApiService } from '../../../core/api/collection-api.service';
import { RecognitionApiService } from '../../../core/api/recognition-api.service';
import { RecognitionResult, RecognizedTrack, TrackMatch } from '../../../core/models/recognition';
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
 * toevoegen, op de verlanglijst zetten of zien dat je hem al hebt. Met route-data {@code mode: 'collection'}
 * (F-32, {@code /collectie/luisteren}): "Welke van mijn platen is dit?" — alleen de eigen collectie, met
 * de track en de opslaglocatie, zonder zoekactie bij Discogs/MusicBrainz.
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
  templateUrl: './listen-page.html',
  styleUrl: './listen-page.scss',
})
export class ListenPage {
  protected readonly recorder = inject(AudioRecorderService);
  private readonly recognitionApi = inject(RecognitionApiService);
  private readonly collectionApi = inject(CollectionApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  protected readonly wishlist = inject(WishlistActions);
  /** F-32: "Welke van mijn platen is dit?" (route-data {@code mode: 'collection'}). */
  protected readonly collectionMode = inject(ActivatedRoute).snapshot.data['mode'] === 'collection';
  protected readonly hint = this.collectionMode
    ? 'Laat het nummer spelen; je ziet op welke plaat of cd in je collectie het staat, en waar die ligt.'
    : 'Laat de plaat spelen en houd je telefoon of laptop in de buurt van de speaker.';
  protected readonly startLabel = this.collectionMode ? 'Welke plaat is dit?' : 'Luisteren';

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
    const scope = this.collectionMode ? 'collection' : 'all';
    this.recognitionApi.recognize(recording.blob, recording.fileName, scope).subscribe({
      next: (result) => this.state.set({ kind: 'result', result }),
      error: (err) => this.state.set({ kind: 'error', message: errorMessage(err) }),
    });
  }

  protected trackFor(result: RecognitionResult, itemId: string): TrackMatch | undefined {
    return result.trackMatches.find((m) => m.itemId === itemId);
  }

  protected searchParams(track: RecognizedTrack): Record<string, string> {
    return { artist: track.artist, title: track.album ?? track.title };
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
