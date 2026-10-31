import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import { CollectionApiService } from '../../core/api/collection-api.service';
import { CollectionPage as CollectionPageDto, CollectionQuery } from '../../core/models/collection';
import { MEDIA_TYPES, MediaType } from '../../core/models/release';
import { Cover } from '../../shared/cover';
import { errorMessage } from '../../shared/http-error';
import { ConditionPipe, MediaTypePipe } from '../../shared/label.pipes';

export type ViewMode = 'grid' | 'list';

export const SORT_OPTIONS = [
  { value: 'added,desc', label: 'Laatst toegevoegd' },
  { value: 'artist,asc', label: 'Artiest A–Z' },
  { value: 'artist,desc', label: 'Artiest Z–A' },
  { value: 'title,asc', label: 'Titel A–Z' },
  { value: 'year,desc', label: 'Jaar (nieuw → oud)' },
  { value: 'year,asc', label: 'Jaar (oud → nieuw)' },
] as const;

const DEFAULT_SORT = 'added,desc';
const PAGE_SIZES = [24, 48, 96];
const VIEW_STORAGE_KEY = 'collection.view';

type Result =
  | { kind: 'loading' }
  | { kind: 'done'; page: CollectionPageDto }
  | { kind: 'error'; message: string };

/**
 * Collectie-overzicht (F-18): grid met covers of lijst, filter op type, zoeken, sorteren en pagineren.
 * Alle keuzes staan in de query-params, zodat terug vanaf een detailpagina op dezelfde plek uitkomt.
 */
@Component({
  selector: 'app-collection-page',
  imports: [
    ConditionPipe,
    Cover,
    MediaTypePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatButtonToggleModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  templateUrl: './collection-page.html',
  styleUrl: './collection-page.scss',
})
export class CollectionPage {
  private readonly collectionApi = inject(CollectionApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly mediaTypes = MEDIA_TYPES;
  protected readonly sortOptions = SORT_OPTIONS;
  protected readonly pageSizes = PAGE_SIZES;

  protected readonly searchControl = new FormControl('', { nonNullable: true });

  readonly query = signal<Required<CollectionQuery>>({
    q: null,
    mediaType: null,
    sort: DEFAULT_SORT,
    page: 0,
    size: PAGE_SIZES[0],
  });
  readonly view = signal<ViewMode>(readStoredView());
  readonly result = signal<Result>({ kind: 'loading' });

  protected readonly page = computed(() => {
    const r = this.result();
    return r.kind === 'done' ? r.page : null;
  });
  protected readonly errorText = computed(() => {
    const r = this.result();
    return r.kind === 'error' ? r.message : '';
  });
  /** Geen filters actief: een lege pagina betekent dan een lege collectie. */
  protected readonly unfiltered = computed(() => !this.query().q && !this.query().mediaType);

  constructor() {
    const destroyRef = inject(DestroyRef);

    this.route.queryParamMap
      .pipe(
        map((params) => parseQuery(params)),
        tap((query) => {
          this.query.set(query);
          this.view.set(
            this.route.snapshot.queryParamMap.get('view') === 'list' ? 'list' : this.view(),
          );
          if (this.searchControl.value !== (query.q ?? '')) {
            this.searchControl.setValue(query.q ?? '', { emitEvent: false });
          }
          this.result.set({ kind: 'loading' });
        }),
        switchMap((query) =>
          this.collectionApi.list(query).pipe(
            map((page): Result => ({ kind: 'done', page })),
            catchError((err) => of<Result>({ kind: 'error', message: errorMessage(err) })),
          ),
        ),
        takeUntilDestroyed(destroyRef),
      )
      .subscribe((result) => this.result.set(result));

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(destroyRef))
      .subscribe((q) => this.navigate({ q: q.trim() || null, page: 0 }, true));
  }

  setMediaType(mediaType: MediaType | null): void {
    this.navigate({ mediaType, page: 0 });
  }

  /** Chip "Alle" heeft de waarde 'ALL'; een chip uitzetten (undefined) betekent ook alle types. */
  onTypeChip(value: MediaType | 'ALL' | undefined): void {
    const mediaType = !value || value === 'ALL' ? null : value;
    if (mediaType !== this.query().mediaType) {
      this.setMediaType(mediaType);
    }
  }

  setSort(sort: string): void {
    this.navigate({ sort, page: 0 });
  }

  onPage(event: PageEvent): void {
    this.navigate({ page: event.pageIndex, size: event.pageSize });
  }

  setView(view: ViewMode): void {
    this.view.set(view);
    try {
      localStorage.setItem(VIEW_STORAGE_KEY, view);
    } catch {
      // geen opslag beschikbaar (privévenster)
    }
    this.navigate({}, true);
  }

  clearFilters(): void {
    this.searchControl.setValue('', { emitEvent: false });
    this.navigate({ q: null, mediaType: null, page: 0 });
  }

  private navigate(changes: Partial<CollectionQuery>, replaceUrl = false): void {
    const next = { ...this.query(), ...changes };
    void this.router.navigate([], {
      relativeTo: this.route,
      replaceUrl,
      queryParams: {
        q: next.q || null,
        type: next.mediaType || null,
        sort: next.sort === DEFAULT_SORT ? null : next.sort,
        page: next.page ? next.page : null,
        size: next.size === PAGE_SIZES[0] ? null : next.size,
        view: this.view() === 'list' ? 'list' : null,
      },
    });
  }
}

function parseQuery(params: ParamMap): Required<CollectionQuery> {
  const type = params.get('type') as MediaType | null;
  const sort = params.get('sort');
  const size = Number(params.get('size'));
  const page = Number(params.get('page'));
  return {
    q: params.get('q'),
    mediaType: type && MEDIA_TYPES.includes(type) ? type : null,
    sort: SORT_OPTIONS.some((o) => o.value === sort) ? (sort as string) : DEFAULT_SORT,
    page: Number.isInteger(page) && page > 0 ? page : 0,
    size: PAGE_SIZES.includes(size) ? size : PAGE_SIZES[0],
  };
}

function readStoredView(): ViewMode {
  try {
    return localStorage.getItem(VIEW_STORAGE_KEY) === 'list' ? 'list' : 'grid';
  } catch {
    return 'grid';
  }
}
