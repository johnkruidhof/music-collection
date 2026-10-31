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
  template: `
    <header class="header">
      <h1>Catalogusbeheer</h1>
      @if (page(); as p) {
        @if (p.unusedCount > 0) {
          <button mat-stroked-button type="button" (click)="deleteUnused(p.unusedCount)">
            <mat-icon>cleaning_services</mat-icon> {{ p.unusedCount }} ongebruikte opruimen
          </button>
        }
      }
    </header>
    <p class="hint">
      De gedeelde catalogus van alle gebruikers. Een release die nog in een collectie of op een
      verlanglijst staat, kun je niet verwijderen; voeg een dubbele release samen met de juiste.
    </p>

    <div class="toolbar">
      <mat-form-field appearance="outline" class="search" subscriptSizing="dynamic">
        <mat-icon matPrefix>search</mat-icon>
        <mat-label>Artiest, titel, barcode of id</mat-label>
        <input matInput [formControl]="query" type="search" autocomplete="off" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="source" subscriptSizing="dynamic">
        <mat-label>Bron</mat-label>
        <mat-select [value]="source()" (selectionChange)="setSource($event.value)">
          <mat-option [value]="null">Alle bronnen</mat-option>
          <mat-option value="DISCOGS">Discogs</mat-option>
          <mat-option value="MUSICBRAINZ">MusicBrainz</mat-option>
          <mat-option value="MANUAL">Handmatig</mat-option>
        </mat-select>
      </mat-form-field>
    </div>

    @if (loading()) {
      <mat-progress-bar mode="indeterminate" />
    }
    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
    @if (page(); as p) {
      <p class="count">{{ p.totalElements }} releases</p>
      <ul class="list">
        @for (release of p.content; track release.id) {
          <li class="row">
            <app-cover class="thumb" [url]="release.coverThumbnailUrl" [alt]="release.title" />
            <div class="info">
              <span class="title">{{ release.artist }} – {{ release.title }}</span>
              <span class="meta">
                {{ release.mediaType | mediaType
                }}{{ release.releaseYear ? ' · ' + release.releaseYear : ''
                }}{{ release.country ? ' · ' + release.country : ''
                }}{{ release.formatDetails ? ' · ' + release.formatDetails : '' }}
              </span>
              <span class="meta">
                {{ release.source | source }}{{ release.discogsId ? ' ' + release.discogsId : ''
                }}{{ release.musicbrainzId ? ' ' + release.musicbrainzId : ''
                }}{{ release.barcode ? ' · barcode ' + release.barcode : '' }} · sinds
                {{ release.createdAt | date: 'd MMM y' }}
              </span>
            </div>
            <div class="usage" [class.unused]="isUnused(release)">
              {{ release.collectionCount }} in collecties<br />{{ release.wishlistCount }} op
              verlanglijsten
            </div>
            <div class="actions">
              <button mat-button type="button" (click)="merge(release)">
                <mat-icon>merge</mat-icon> Samenvoegen…
              </button>
              @if (isUnused(release)) {
                <button mat-button type="button" class="delete" (click)="remove(release)">
                  <mat-icon>delete</mat-icon> Verwijderen
                </button>
              }
            </div>
          </li>
        }
      </ul>
      @if (p.totalElements > p.size) {
        <mat-paginator
          [length]="p.totalElements"
          [pageIndex]="p.page"
          [pageSize]="p.size"
          [pageSizeOptions]="[25, 50, 100]"
          (page)="onPage($event)"
        />
      }
    }
  `,
  styles: `
    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;
    }
    .hint,
    .count,
    .meta {
      color: var(--mat-sys-on-surface-variant);
    }
    .toolbar {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      margin-bottom: 12px;
    }
    .search {
      flex: 1 1 280px;
    }
    .source {
      flex: 0 1 200px;
    }
    .list {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .row {
      display: grid;
      grid-template-columns: 48px 1fr auto;
      grid-template-areas: 'thumb info usage' 'thumb actions actions';
      gap: 4px 16px;
      align-items: center;
      padding: 8px 0;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .thumb {
      grid-area: thumb;
      width: 48px;
      align-self: start;
    }
    .info {
      grid-area: info;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }
    .title {
      font-weight: 500;
      overflow-wrap: anywhere;
    }
    .meta {
      font-size: 0.85rem;
      overflow-wrap: anywhere;
    }
    .usage {
      grid-area: usage;
      text-align: right;
      font-size: 0.85rem;
      font-variant-numeric: tabular-nums;
    }
    .usage.unused {
      color: var(--mat-sys-tertiary);
    }
    .actions {
      grid-area: actions;
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }
    .delete {
      color: var(--mat-sys-error);
    }
    .error {
      color: var(--mat-sys-error);
    }
  `,
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
