import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { CollectionApiService } from '../../../core/api/collection-api.service';
import { LookupApiService } from '../../../core/api/lookup-api.service';
import {
  MEDIA_TYPES,
  MediaType,
  Release,
  ReleaseCandidate,
  ReleaseSearchCriteria,
} from '../../../core/models/release';
import { CandidateCard } from '../../../shared/candidate-card';
import { errorMessage } from '../../../shared/http-error';
import { DurationPipe, MediaTypePipe } from '../../../shared/label.pipes';

type Results =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'done'; candidates: ReleaseCandidate[] }
  | { kind: 'error'; message: string };

type TrackState = { kind: 'loading' } | { kind: 'loaded'; release: Release } | { kind: 'error' };

const candidateKey = (c: ReleaseCandidate) => `${c.source}:${c.externalId}`;

/**
 * Zoeken zonder barcode (F-16). De zoekopdracht staat in de query-params, zodat terug/verversen werkt
 * en de scan-flow hierheen kan doorverwijzen ({@code ?barcode=}).
 */
@Component({
  selector: 'app-search-page',
  imports: [
    CandidateCard,
    DurationPipe,
    MediaTypePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  template: `
    @if (barcode()) {
      <p class="notice">
        <mat-icon>info</mat-icon>
        Barcode {{ barcode() }} is niet gevonden. Zoek op artiest en titel om de juiste persing te
        kiezen.
      </p>
    }

    <form class="form" [formGroup]="form" (ngSubmit)="submit()">
      <mat-form-field appearance="outline">
        <mat-label>Artiest</mat-label>
        <input matInput formControlName="artist" autocomplete="off" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Titel</mat-label>
        <input matInput formControlName="title" autocomplete="off" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="narrow">
        <mat-label>Jaar</mat-label>
        <input matInput formControlName="year" inputmode="numeric" autocomplete="off" />
        @if (form.controls.year.invalid) {
          <mat-error>1900–2100</mat-error>
        }
      </mat-form-field>
      <mat-form-field appearance="outline" class="narrow">
        <mat-label>Type</mat-label>
        <mat-select formControlName="mediaType">
          <mat-option [value]="null">Alle types</mat-option>
          @for (type of mediaTypes; track type) {
            <mat-option [value]="type">{{ type | mediaType }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <div class="submit">
        <button mat-flat-button type="submit"><mat-icon>search</mat-icon> Zoeken</button>
        @if (formError()) {
          <span class="error">{{ formError() }}</span>
        }
      </div>
    </form>

    @switch (results().kind) {
      @case ('loading') {
        <mat-progress-bar mode="indeterminate" />
      }
      @case ('error') {
        <p class="error">{{ resultError() }}</p>
      }
      @case ('done') {
        @if (candidates().length === 0) {
          <p>Niets gevonden. Probeer minder of andere zoektermen, of voer de release zelf in.</p>
        } @else {
          <p class="count">{{ candidates().length }} resultaten</p>
          <div class="candidates">
            @for (candidate of candidates(); track key(candidate)) {
              <app-candidate-card
                [candidate]="candidate"
                [busy]="adding() === key(candidate)"
                [added]="added().has(key(candidate))"
                (add)="add($event)"
              >
                <button mat-button type="button" (click)="toggleTracks(candidate)">
                  <mat-icon>{{
                    tracks()[key(candidate)] ? 'expand_less' : 'queue_music'
                  }}</mat-icon>
                  Tracks
                </button>
              </app-candidate-card>
              @if (tracks()[key(candidate)]; as state) {
                <div class="tracks">
                  @switch (state.kind) {
                    @case ('loading') {
                      <mat-progress-bar mode="indeterminate" />
                    }
                    @case ('error') {
                      <p class="error">Tracks konden niet opgehaald worden.</p>
                    }
                    @case ('loaded') {
                      @if (state.kind === 'loaded' && state.release.tracks.length > 0) {
                        <ol>
                          @for (track of state.release.tracks; track track.sortOrder) {
                            <li>
                              <span class="pos">{{ track.position }}</span>
                              <span class="track-title">{{ track.title }}</span>
                              <span class="dur">{{ track.durationSeconds | duration }}</span>
                            </li>
                          }
                        </ol>
                      } @else {
                        <p>Geen tracklist bekend.</p>
                      }
                    }
                  }
                </div>
              }
            }
          </div>
        }
      }
    }

    @if (results().kind !== 'idle') {
      <p class="manual">
        Staat je release er niet tussen?
        <a mat-button routerLink="../handmatig" [queryParams]="manualParams()">
          <mat-icon>edit</mat-icon> Handmatig invoeren
        </a>
      </p>
    }
  `,
  styles: `
    .form {
      display: flex;
      flex-wrap: wrap;
      gap: 0 12px;
      align-items: flex-start;
    }
    .form mat-form-field {
      flex: 1 1 220px;
    }
    .form .narrow {
      flex: 0 1 160px;
    }
    .submit {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1 1 100%;
      margin-bottom: 16px;
    }
    .notice {
      display: flex;
      gap: 8px;
      align-items: center;
      padding: 12px;
      border-radius: 8px;
      background: var(--mat-sys-secondary-container);
      color: var(--mat-sys-on-secondary-container);
    }
    .candidates {
      display: grid;
      gap: 12px;
    }
    .count {
      color: var(--mat-sys-on-surface-variant);
    }
    .tracks {
      margin: -4px 0 4px;
      padding: 8px 16px;
      border-radius: 0 0 12px 12px;
      background: var(--mat-sys-surface-container);
    }
    .tracks ol {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .tracks li {
      display: grid;
      grid-template-columns: 3.5em 1fr auto;
      gap: 8px;
      padding: 2px 0;
    }
    .pos,
    .dur {
      color: var(--mat-sys-on-surface-variant);
      font-variant-numeric: tabular-nums;
    }
    .error {
      color: var(--mat-sys-error);
    }
    .manual {
      margin-top: 24px;
    }
  `,
})
export class SearchPage {
  private readonly lookupApi = inject(LookupApiService);
  private readonly collectionApi = inject(CollectionApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly mediaTypes = MEDIA_TYPES;
  protected readonly key = candidateKey;

  protected readonly form = new FormGroup({
    artist: new FormControl('', { nonNullable: true }),
    title: new FormControl('', { nonNullable: true }),
    year: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^\s*(19|20)\d\d\s*$|^\s*2100\s*$|^\s*$/)],
    }),
    mediaType: new FormControl<MediaType | null>(null),
  });

  readonly results = signal<Results>({ kind: 'idle' });
  readonly adding = signal<string | null>(null);
  readonly added = signal<ReadonlySet<string>>(new Set());
  readonly tracks = signal<Record<string, TrackState>>({});
  protected readonly barcode = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);

  protected readonly candidates = computed(() => {
    const r = this.results();
    return r.kind === 'done' ? r.candidates : [];
  });
  protected readonly resultError = computed(() => {
    const r = this.results();
    return r.kind === 'error' ? r.message : '';
  });

  private searchSubscription?: Subscription;

  constructor() {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe((params) => this.onQueryParams(params));
  }

  /** Zet de zoekopdracht in de URL; het zoeken zelf volgt uit de query-params. */
  submit(): void {
    const { artist, title, year, mediaType } = this.form.getRawValue();
    if (this.form.invalid) {
      return;
    }
    if (!artist.trim() && !title.trim()) {
      this.formError.set('Vul minstens een artiest of een titel in.');
      return;
    }
    this.formError.set(null);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        artist: artist.trim() || null,
        title: title.trim() || null,
        year: year.trim() || null,
        mediaType: mediaType ?? null,
        barcode: this.barcode(),
        // zelfde zoekopdracht opnieuw → toch opnieuw zoeken
        t: Date.now(),
      },
    });
  }

  add(candidate: ReleaseCandidate): void {
    if (this.adding()) {
      return;
    }
    const key = candidateKey(candidate);
    this.adding.set(key);
    this.collectionApi.addCandidate(candidate).subscribe({
      next: (item) => {
        this.adding.set(null);
        this.added.update((set) => new Set(set).add(key));
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

  toggleTracks(candidate: ReleaseCandidate): void {
    const key = candidateKey(candidate);
    if (this.tracks()[key]) {
      this.tracks.update((all) => {
        const rest = { ...all };
        delete rest[key];
        return rest;
      });
      return;
    }
    this.setTracks(key, { kind: 'loading' });
    this.lookupApi.details(candidate.source, candidate.externalId).subscribe({
      next: (release) => this.setTracks(key, { kind: 'loaded', release }),
      error: () => this.setTracks(key, { kind: 'error' }),
    });
  }

  protected manualParams(): Record<string, string | null> {
    const { artist, title, year, mediaType } = this.form.getRawValue();
    return {
      artist: artist.trim() || null,
      title: title.trim() || null,
      year: year.trim() || null,
      mediaType,
      barcode: this.barcode(),
    };
  }

  private setTracks(key: string, state: TrackState): void {
    this.tracks.update((all) => ({ ...all, [key]: state }));
  }

  private onQueryParams(params: ParamMap): void {
    this.barcode.set(params.get('barcode'));
    const criteria: ReleaseSearchCriteria = {
      artist: params.get('artist'),
      title: params.get('title'),
      year: params.get('year') ? Number(params.get('year')) : null,
      mediaType: (params.get('mediaType') as MediaType | null) ?? null,
    };
    this.form.setValue({
      artist: criteria.artist ?? '',
      title: criteria.title ?? '',
      year: params.get('year') ?? '',
      mediaType: criteria.mediaType ?? null,
    });
    if (criteria.artist || criteria.title) {
      this.search(criteria);
    }
  }

  private search(criteria: ReleaseSearchCriteria): void {
    this.searchSubscription?.unsubscribe();
    this.results.set({ kind: 'loading' });
    this.tracks.set({});
    this.searchSubscription = this.lookupApi.search(criteria).subscribe({
      next: (candidates) => this.results.set({ kind: 'done', candidates }),
      error: (err) => this.results.set({ kind: 'error', message: errorMessage(err) }),
    });
  }
}
