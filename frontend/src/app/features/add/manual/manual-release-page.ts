import { Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, of, switchMap } from 'rxjs';
import { CollectionApiService } from '../../../core/api/collection-api.service';
import { CollectionItem } from '../../../core/models/collection';
import { MEDIA_TYPES, ManualRelease, MediaType, Release } from '../../../core/models/release';
import {
  CollectionFieldsFormComponent,
  collectionFieldsValue,
  createCollectionFieldsForm,
} from '../../../shared/collection-fields-form';
import { Cover } from '../../../shared/cover';
import { errorMessage } from '../../../shared/http-error';
import { resizeImage } from '../../../shared/image-resize';
import { MediaTypePipe } from '../../../shared/label.pipes';
import { formatDuration, parseDuration } from '../../../shared/labels';
import { discFromPosition, parseTrackLines } from '../../../shared/track-parser';

type TrackForm = FormGroup<{
  discNumber: FormControl<number>;
  position: FormControl<string>;
  title: FormControl<string>;
  duration: FormControl<string>;
  artist: FormControl<string>;
}>;

function durationValidator(control: AbstractControl<string>): ValidationErrors | null {
  return parseDuration(control.value) === undefined ? { duration: true } : null;
}

/**
 * Handmatig een release invoeren (F-17) of een handmatige release bewerken. Zonder {@link id}:
 * toevoegen (`/toevoegen/handmatig`, voorinvullen via query-params uit scan/zoeken); met {@link id}:
 * bewerken (`/collectie/:id/bewerken`).
 */
@Component({
  selector: 'app-manual-release-page',
  imports: [
    CollectionFieldsFormComponent,
    Cover,
    MediaTypePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  templateUrl: './manual-release-page.html',
  styleUrl: './manual-release-page.scss',
})
export class ManualReleasePage {
  private readonly collectionApi = inject(CollectionApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  /** Route-parameter (component input binding); gezet = bewerken. */
  readonly id = input<string>();
  protected readonly editing = computed(() => !!this.id());

  protected readonly mediaTypes = MEDIA_TYPES;

  readonly form = new FormGroup({
    artist: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(500)],
    }),
    title: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(500)],
    }),
    releaseYear: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^\s*(19\d\d|20\d\d|2100)?\s*$/)],
    }),
    mediaType: new FormControl<MediaType | null>(null, Validators.required),
    formatDetails: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(255)],
    }),
    label: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
    catalogNumber: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(100)],
    }),
    country: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(100)] }),
    barcode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^[\d\s-]{0,32}$/)],
    }),
    tracks: new FormArray<TrackForm>([]),
  });
  readonly fields = createCollectionFieldsForm();
  protected readonly pasteText = new FormControl('', { nonNullable: true });

  readonly loading = signal(false);
  readonly saving = signal(false);
  protected readonly loadError = signal<string | null>(null);
  /** Bewerken van een release van Discogs/MusicBrainz kan niet. */
  protected readonly notManual = signal(false);

  /** Nieuw gekozen cover (al verkleind) met preview; {@code removeCover} = bestaande weghalen. */
  readonly coverBlob = signal<Blob | null>(null);
  protected readonly coverPreview = signal<string | null>(null);
  readonly removeCover = signal(false);
  private readonly existingCover = signal<string | null>(null);
  protected readonly shownCover = computed(
    () => this.coverPreview() ?? (this.removeCover() ? null : this.existingCover()),
  );
  protected readonly coverError = signal<string | null>(null);

  constructor() {
    const destroyRef = inject(DestroyRef);
    destroyRef.onDestroy(() => this.revokePreview());

    effect(() => {
      const id = this.id();
      if (id) {
        this.load(id);
      } else {
        this.prefill();
      }
    });
  }

  get tracks(): FormArray<TrackForm> {
    return this.form.controls.tracks;
  }

  addTrack(
    values?: Partial<{
      discNumber: number;
      position: string;
      title: string;
      duration: string;
      artist: string;
    }>,
  ): void {
    const previous = this.tracks.at(this.tracks.length - 1)?.getRawValue();
    this.tracks.push(
      new FormGroup({
        discNumber: new FormControl(values?.discNumber ?? previous?.discNumber ?? 1, {
          nonNullable: true,
          validators: [Validators.required, Validators.min(1), Validators.max(99)],
        }),
        position: new FormControl(values?.position ?? '', {
          nonNullable: true,
          validators: [Validators.maxLength(20)],
        }),
        title: new FormControl(values?.title ?? '', {
          nonNullable: true,
          validators: [Validators.required, Validators.maxLength(500)],
        }),
        duration: new FormControl(values?.duration ?? '', {
          nonNullable: true,
          validators: [durationValidator],
        }),
        artist: new FormControl(values?.artist ?? '', {
          nonNullable: true,
          validators: [Validators.maxLength(500)],
        }),
      }),
    );
  }

  removeTrack(index: number): void {
    this.tracks.removeAt(index);
  }

  moveTrack(index: number, delta: -1 | 1): void {
    const target = index + delta;
    if (target < 0 || target >= this.tracks.length) {
      return;
    }
    const track = this.tracks.at(index);
    this.tracks.removeAt(index);
    this.tracks.insert(target, track);
  }

  /** Schijfnummer automatisch afleiden als de positie verandert ("C1" → 2). */
  onPositionChange(index: number): void {
    const track = this.tracks.at(index);
    const position = track.controls.position.value.trim();
    if (position) {
      track.controls.discNumber.setValue(discFromPosition(position));
    }
  }

  /** Geplakte tracklist (één per regel) achter de bestaande tracks zetten. */
  importPastedTracks(): void {
    const parsed = parseTrackLines(this.pasteText.value);
    for (const track of parsed) {
      this.addTrack({
        discNumber: track.discNumber,
        position: track.position ?? '',
        title: track.title,
        duration: track.duration ?? '',
      });
    }
    this.pasteText.reset();
    if (parsed.length > 0) {
      this.snackBar.open(`${parsed.length} tracks toegevoegd`, undefined, { duration: 2500 });
    }
  }

  async onCoverSelected(event: Event): Promise<void> {
    const fileInput = event.target as HTMLInputElement;
    const file = fileInput.files?.[0];
    fileInput.value = '';
    if (!file) {
      return;
    }
    this.coverError.set(null);
    try {
      const blob = await resizeImage(file);
      this.revokePreview();
      this.coverBlob.set(blob);
      this.coverPreview.set(URL.createObjectURL(blob));
      this.removeCover.set(false);
    } catch (err) {
      this.coverError.set(
        err instanceof Error ? err.message : 'Afbeelding kon niet gelezen worden.',
      );
    }
  }

  clearCover(): void {
    this.revokePreview();
    this.coverBlob.set(null);
    this.removeCover.set(!!this.existingCover());
  }

  /** Formulierwaarde → request voor de backend. */
  releaseValue(): ManualRelease {
    const v = this.form.getRawValue();
    const text = (value: string) => (value.trim() === '' ? null : value.trim());
    return {
      barcode: text(v.barcode),
      artist: v.artist.trim(),
      title: v.title.trim(),
      releaseYear: text(v.releaseYear) ? Number(v.releaseYear.trim()) : null,
      mediaType: v.mediaType as MediaType,
      formatDetails: text(v.formatDetails),
      label: text(v.label),
      catalogNumber: text(v.catalogNumber),
      country: text(v.country),
      tracks: v.tracks.map((t, i) => ({
        discNumber: t.discNumber || 1,
        position: text(t.position),
        sortOrder: i + 1,
        title: t.title.trim(),
        durationSeconds: parseDuration(t.duration) ?? null,
        artist: text(t.artist),
      })),
    };
  }

  save(): void {
    this.form.markAllAsTouched();
    this.fields.markAllAsTouched();
    if (this.form.invalid || this.fields.invalid || this.saving()) {
      this.snackBar.open('Controleer de gemarkeerde velden.', 'OK', { duration: 4000 });
      return;
    }
    this.saving.set(true);
    const id = this.id();
    const saved$ = id
      ? this.collectionApi.updateRelease(id, this.releaseValue())
      : this.collectionApi.add({
          release: this.releaseValue(),
          fields: collectionFieldsValue(this.fields),
        });

    saved$.pipe(switchMap((item) => this.saveCover(item))).subscribe({
      next: (item) => {
        this.saving.set(false);
        this.form.markAsPristine();
        this.snackBar.open(
          id
            ? 'Wijzigingen opgeslagen'
            : `Toegevoegd: ${item.release.artist} – ${item.release.title}`,
          undefined,
          { duration: 3000 },
        );
        void this.router.navigate(['/collectie', item.id]);
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 });
      },
    });
  }

  private saveCover(item: CollectionItem): Observable<CollectionItem> {
    const blob = this.coverBlob();
    if (blob) {
      return this.collectionApi.uploadCover(item.id, blob);
    }
    if (this.removeCover()) {
      return this.collectionApi.deleteCover(item.id);
    }
    return of(item);
  }

  private load(id: string): void {
    this.loading.set(true);
    this.collectionApi.get(id).subscribe({
      next: (item) => {
        this.loading.set(false);
        if (item.release.source !== 'MANUAL') {
          this.notManual.set(true);
          return;
        }
        this.patchRelease(item.release);
        this.existingCover.set(item.release.coverImageUrl);
      },
      error: (err) => {
        this.loading.set(false);
        this.loadError.set(errorMessage(err));
      },
    });
  }

  private patchRelease(release: Release): void {
    this.form.patchValue({
      artist: release.artist,
      title: release.title,
      releaseYear: release.releaseYear ? String(release.releaseYear) : '',
      mediaType: release.mediaType,
      formatDetails: release.formatDetails ?? '',
      label: release.label ?? '',
      catalogNumber: release.catalogNumber ?? '',
      country: release.country ?? '',
      barcode: release.barcode ?? '',
    });
    this.tracks.clear();
    for (const track of release.tracks) {
      this.addTrack({
        discNumber: track.discNumber,
        position: track.position ?? '',
        title: track.title,
        duration: formatDuration(track.durationSeconds),
        artist: track.artist ?? '',
      });
    }
    this.form.markAsPristine();
  }

  /** Gegevens uit de scan-/zoek-flow overnemen (?barcode=&artist=&title=&year=&mediaType=). */
  private prefill(): void {
    const params = this.route.snapshot.queryParamMap;
    const mediaType = params.get('mediaType') as MediaType | null;
    this.form.patchValue({
      barcode: params.get('barcode') ?? '',
      artist: params.get('artist') ?? '',
      title: params.get('title') ?? '',
      releaseYear: params.get('year') ?? '',
      mediaType: mediaType && MEDIA_TYPES.includes(mediaType) ? mediaType : null,
    });
  }

  private revokePreview(): void {
    const preview = this.coverPreview();
    if (preview) {
      URL.revokeObjectURL(preview);
    }
    this.coverPreview.set(null);
  }
}
