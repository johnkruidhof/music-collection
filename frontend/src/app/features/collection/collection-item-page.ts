import { DatePipe, Location } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { filter, switchMap } from 'rxjs';
import { CollectionApiService } from '../../core/api/collection-api.service';
import { CollectionItem } from '../../core/models/collection';
import { Release, Track } from '../../core/models/release';
import {
  CollectionFieldsFormComponent,
  collectionFieldsValue,
  createCollectionFieldsForm,
} from '../../shared/collection-fields-form';
import { ConfirmDialog, ConfirmDialogData } from '../../shared/confirm-dialog';
import { Cover } from '../../shared/cover';
import { errorMessage } from '../../shared/http-error';
import { DurationPipe, MediaTypePipe, SourcePipe } from '../../shared/label.pipes';

export interface Disc {
  number: number;
  tracks: Track[];
}

/** Tracks per disc, in volgorde. */
export function groupByDisc(tracks: Track[]): Disc[] {
  const discs = new Map<number, Track[]>();
  for (const track of [...tracks].sort(
    (a, b) => a.discNumber - b.discNumber || a.sortOrder - b.sortOrder,
  )) {
    discs.set(track.discNumber, [...(discs.get(track.discNumber) ?? []), track]);
  }
  return [...discs].map(([number, discTracks]) => ({ number, tracks: discTracks }));
}

/** Link naar de release bij de bron, of {@code null} voor handmatige releases. */
export function sourceUrl(release: Release): string | null {
  if (release.source === 'DISCOGS' && release.discogsId) {
    return `https://www.discogs.com/release/${release.discogsId}`;
  }
  if (release.source === 'MUSICBRAINZ' && release.musicbrainzId) {
    return `https://musicbrainz.org/release/${release.musicbrainzId}`;
  }
  return null;
}

/** Detailpagina (F-19): release met tracklist en de eigen velden van dit exemplaar. */
@Component({
  selector: 'app-collection-item-page',
  imports: [
    CollectionFieldsFormComponent,
    Cover,
    DatePipe,
    DurationPipe,
    MediaTypePipe,
    RouterLink,
    SourcePipe,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './collection-item-page.html',
  styleUrl: './collection-item-page.scss',
})
export class CollectionItemPage {
  private readonly collectionApi = inject(CollectionApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  /** Route-parameter (component input binding). */
  readonly id = input.required<string>();

  readonly item = signal<CollectionItem | null>(null);
  protected readonly loadError = signal<string | null>(null);
  readonly saving = signal(false);
  readonly fields = createCollectionFieldsForm();

  protected readonly release = computed(() => this.item()?.release ?? null);
  protected readonly discs = computed(() => groupByDisc(this.release()?.tracks ?? []));
  protected readonly totalDuration = computed(() => {
    const durations = (this.release()?.tracks ?? []).map((t) => t.durationSeconds);
    return durations.length > 0 && durations.every((d) => d !== null)
      ? durations.reduce((sum: number, d) => sum + (d ?? 0), 0)
      : null;
  });
  protected readonly externalUrl = computed(() => {
    const release = this.release();
    return release ? sourceUrl(release) : null;
  });

  constructor() {
    effect(() => this.load(this.id()));
  }

  saveFields(): void {
    const item = this.item();
    this.fields.markAllAsTouched();
    if (!item || this.fields.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.collectionApi.update(item.id, collectionFieldsValue(this.fields)).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.item.set(updated);
        this.fields.markAsPristine();
        this.snackBar.open('Opgeslagen', undefined, { duration: 2500 });
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 });
      },
    });
  }

  resetFields(): void {
    const item = this.item();
    if (item) {
      this.fields.reset(createCollectionFieldsForm(item).getRawValue());
    }
  }

  confirmDelete(): void {
    const item = this.item();
    if (!item) {
      return;
    }
    this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
        data: {
          title: 'Verwijderen uit je collectie?',
          message: `${item.release.artist} – ${item.release.title} wordt uit je collectie verwijderd.`,
          confirmLabel: 'Verwijderen',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed === true),
        switchMap(() => this.collectionApi.delete(item.id)),
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Verwijderd uit je collectie', undefined, { duration: 3000 });
          void this.router.navigate(['/collectie']);
        },
        error: (err) => this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 }),
      });
  }

  /** Terug naar het overzicht met dezelfde filters als we daarvandaan kwamen. */
  back(): void {
    if (this.router.lastSuccessfulNavigation()?.previousNavigation) {
      this.location.back();
    } else {
      void this.router.navigate(['/collectie']);
    }
  }

  private load(id: string): void {
    this.item.set(null);
    this.loadError.set(null);
    this.collectionApi.get(id).subscribe({
      next: (item) => {
        this.item.set(item);
        this.fields.reset(createCollectionFieldsForm(item).getRawValue());
      },
      error: (err) => this.loadError.set(errorMessage(err)),
    });
  }
}
