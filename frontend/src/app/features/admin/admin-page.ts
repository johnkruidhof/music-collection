import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs';
import { AdminApiService } from '../../core/api/admin-api.service';
import { AdminRelease, AdminReleasePage } from '../../core/models/admin';
import { MetadataSource } from '../../core/models/release';
import { ConfirmDialog, ConfirmDialogData } from '../../shared/confirm-dialog';
import { Cover } from '../../shared/cover';
import { errorMessage } from '../../shared/http-error';
import { MediaTypePipe, SourcePipe } from '../../shared/label.pipes';
import { MergeDialog } from './merge-dialog';

/** Catalogusbeheer (F-27, rol admin): doorzoeken, ongebruikte releases opruimen, dubbelen samenvoegen. */
@Component({
  selector: 'app-admin-page',
  imports: [
    Cover,
    DatePipe,
    MediaTypePipe,
    ReactiveFormsModule,
    SourcePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  templateUrl: './admin-page.html',
  styleUrl: './admin-page.scss',
})
export class AdminPage {
  private readonly adminApi = inject(AdminApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly query = new FormControl('', { nonNullable: true });
  readonly source = signal<MetadataSource | null>(null);
  readonly page = signal<AdminReleasePage | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  private readonly pageIndex = signal(0);
  private readonly pageSize = signal(25);

  protected readonly unusedCount = computed(() => this.page()?.unusedCount ?? 0);

  constructor() {
    this.query.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => {
        this.pageIndex.set(0);
        this.load();
      });
    this.load();
  }

  isUnused(release: AdminRelease): boolean {
    return release.collectionCount === 0 && release.wishlistCount === 0;
  }

  setSource(source: MetadataSource | null): void {
    this.source.set(source);
    this.pageIndex.set(0);
    this.load();
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.adminApi
      .search(this.query.value.trim() || null, this.source(), this.pageIndex(), this.pageSize())
      .subscribe({
        next: (page) => {
          this.loading.set(false);
          this.error.set(null);
          this.page.set(page);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(errorMessage(err));
        },
      });
  }

  merge(release: AdminRelease): void {
    this.dialog
      .open<MergeDialog, AdminRelease, AdminRelease>(MergeDialog, { data: release, width: '640px' })
      .afterClosed()
      .pipe(
        filter((target): target is AdminRelease => !!target),
        switchMap((target) => this.adminApi.merge(release.id, target.id)),
      )
      .subscribe({
        next: (target) => {
          this.snackBar.open(`Samengevoegd met ${target.artist} – ${target.title}`, undefined, {
            duration: 4000,
          });
          this.load();
        },
        error: (err) => this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 }),
      });
  }

  remove(release: AdminRelease): void {
    this.confirm({
      title: 'Release verwijderen?',
      message: `${release.artist} – ${release.title} wordt uit de catalogus verwijderd.`,
      confirmLabel: 'Verwijderen',
    })
      .pipe(switchMap(() => this.adminApi.delete(release.id)))
      .subscribe({
        next: () => this.load(),
        error: (err) => this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 }),
      });
  }

  deleteUnused(count: number): void {
    this.confirm({
      title: 'Ongebruikte releases opruimen?',
      message: `${count} releases die in geen enkele collectie of verlanglijst staan worden verwijderd.`,
      confirmLabel: 'Opruimen',
    })
      .pipe(switchMap(() => this.adminApi.deleteUnused()))
      .subscribe({
        next: ({ deleted }) => {
          this.snackBar.open(`${deleted} releases opgeruimd`, undefined, { duration: 4000 });
          this.load();
        },
        error: (err) => this.snackBar.open(errorMessage(err), 'OK', { duration: 6000 }),
      });
  }

  private confirm(data: ConfirmDialogData) {
    return this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, { data })
      .afterClosed()
      .pipe(filter((confirmed) => confirmed === true));
  }
}
