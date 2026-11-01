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
  templateUrl: './import-export-page.html',
  styleUrl: './import-export-page.scss',
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
