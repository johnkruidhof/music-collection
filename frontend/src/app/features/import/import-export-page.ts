import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription, filter, switchMap, takeWhile, timer } from 'rxjs';
import { CollectionApiService } from '../../core/api/collection-api.service';
import { ImportJob } from '../../core/models/import-job';
import { saveBlob } from '../../shared/download';
import { errorMessage } from '../../shared/http-error';

export const POLL_INTERVAL_MS = 2000;

/** Exporteren als CSV en importeren uit een Discogs-collectie-export (F-22). */
@Component({
  selector: 'app-import-export-page',
  imports: [DecimalPipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <a mat-button routerLink="/collectie"><mat-icon>arrow_back</mat-icon> Collectie</a>
    <h1>Importeren en exporteren</h1>

    <section>
      <h2>Exporteren</h2>
      <p>Download je hele collectie als CSV-bestand (opent in Excel of Numbers).</p>
      <button mat-stroked-button type="button" [disabled]="exporting()" (click)="export()">
        <mat-icon>download</mat-icon> Collectie exporteren
      </button>
    </section>

    <section>
      <h2>Importeren uit Discogs</h2>
      <p>
        Exporteer op Discogs je collectie (<em>Collection → Export</em>) en kies hier het
        CSV-bestand. De gegevens worden per release bij Discogs opgehaald; dat duurt ongeveer een
        seconde per release. Opnieuw importeren is veilig: releases die je al hebt worden
        overgeslagen.
      </p>
      <input
        #fileInput
        type="file"
        accept=".csv,text/csv"
        hidden
        (change)="onFileSelected($event)"
      />
      <button
        mat-flat-button
        type="button"
        [disabled]="running() || uploading()"
        (click)="fileInput.click()"
      >
        <mat-icon>upload_file</mat-icon> Discogs-export kiezen
      </button>

      @if (job(); as j) {
        <div class="job" aria-live="polite">
          <div class="job-title">
            @switch (j.status) {
              @case ('RUNNING') {
                Bezig met importeren… {{ j.processed }} van {{ j.total }}
              }
              @case ('FINISHED') {
                <mat-icon>check_circle</mat-icon> Import klaar
              }
              @case ('FAILED') {
                <mat-icon>error</mat-icon> Import afgebroken
              }
            }
          </div>
          <mat-progress-bar
            [mode]="j.status === 'RUNNING' && j.processed === 0 ? 'indeterminate' : 'determinate'"
            [value]="progress()"
          />
          <ul class="counts">
            <li>
              <strong>{{ j.imported | number }}</strong> toegevoegd
            </li>
            <li>
              <strong>{{ j.skipped | number }}</strong> stonden al in je collectie
            </li>
            <li>
              <strong>{{ j.failed | number }}</strong> mislukt
            </li>
          </ul>
          @if (j.errors.length > 0) {
            <details>
              <summary>Mislukte regels ({{ j.errors.length }})</summary>
              <ul class="errors">
                @for (error of j.errors; track $index) {
                  <li>{{ error }}</li>
                }
              </ul>
            </details>
          }
          @if (j.status !== 'RUNNING' && j.imported > 0) {
            <a mat-button routerLink="/collectie">Naar je collectie</a>
          }
        </div>
      }
    </section>
  `,
  styles: `
    section {
      margin-bottom: 32px;
      max-width: 720px;
    }
    h2 {
      font-size: 1.15rem;
    }
    .job {
      margin-top: 16px;
      padding: 16px;
      border-radius: 12px;
      background: var(--mat-sys-surface-container-low);
      display: grid;
      gap: 12px;
    }
    .job-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
    }
    .counts {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 24px;
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .errors {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.9rem;
    }
  `,
})
export class ImportExportPage {
  private readonly collectionApi = inject(CollectionApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly job = signal<ImportJob | null>(null);
  readonly exporting = signal(false);
  readonly uploading = signal(false);
  protected readonly running = computed(() => this.job()?.status === 'RUNNING');
  protected readonly progress = computed(() => {
    const j = this.job();
    return j && j.total > 0 ? (j.processed / j.total) * 100 : 0;
  });

  private polling?: Subscription;

  constructor() {
    // Een import die nog loopt (bv. na verversen) weer oppakken.
    this.collectionApi
      .latestImport()
      .pipe(filter((job): job is ImportJob => job !== null))
      .subscribe((job) => this.track(job));
  }

  export(): void {
    this.exporting.set(true);
    this.collectionApi.exportCsv().subscribe({
      next: ({ blob, fileName }) => {
        this.exporting.set(false);
        saveBlob(blob, fileName);
      },
      error: (err) => {
        this.exporting.set(false);
        this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 });
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      this.start(file);
    }
  }

  start(file: File): void {
    this.uploading.set(true);
    this.collectionApi.importDiscogs(file).subscribe({
      next: (job) => {
        this.uploading.set(false);
        this.track(job);
      },
      error: (err) => {
        this.uploading.set(false);
        this.snackBar.open(errorMessage(err), 'OK', { duration: 8000 });
      },
    });
  }

  private track(job: ImportJob): void {
    this.job.set(job);
    this.polling?.unsubscribe();
    if (job.status !== 'RUNNING') {
      return;
    }
    this.polling = timer(POLL_INTERVAL_MS, POLL_INTERVAL_MS)
      .pipe(
        switchMap(() => this.collectionApi.importStatus(job.id)),
        takeWhile((current) => current.status === 'RUNNING', true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (current) => this.job.set(current),
        error: (err) => this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 }),
      });
  }
}
