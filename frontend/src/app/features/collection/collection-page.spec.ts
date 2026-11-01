import { TestBed } from '@angular/core/testing';
import { GenreStore } from '../../shared/genre-store';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CollectionApiService } from '../../core/api/collection-api.service';
import { CollectionItemSummary, CollectionPage as PageDto } from '../../core/models/collection';
import { CollectionPage, groupByGenre } from './collection-page';

const item = (id: string, title: string): CollectionItemSummary => ({
  id,
  releaseId: 'r-' + id,
  artist: 'Radiohead',
  title,
  releaseYear: 1997,
  mediaType: 'VINYL',
  formatDetails: '2×Vinyl, LP',
  coverThumbnailUrl: null,
  conditionMedia: 'NEAR_MINT',
  storageLocation: 'Kast A',
  genreId: null,
  genreName: null,
  subgenreName: null,
  addedAt: '2026-10-31T12:00:00Z',
});

const page = (content: CollectionItemSummary[], totalElements = content.length): PageDto => ({
  content,
  page: 0,
  size: 24,
  totalElements,
  totalPages: Math.ceil(totalElements / 24),
});

describe('CollectionPage', () => {
  const collectionApi = {
    list: vi.fn(() => of(page([item('1', 'OK Computer'), item('2', 'Kid A')]))),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    collectionApi.list.mockReturnValue(of(page([item('1', 'OK Computer'), item('2', 'Kid A')])));
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [CollectionPage],
      providers: [
        provideRouter([{ path: 'collectie', component: CollectionPage }]),
        { provide: CollectionApiService, useValue: collectionApi },
        {
          provide: GenreStore,
          useValue: {
            ensureLoaded: () => undefined,
            options: () => [],
            genres: () => [],
            matches: () => true,
          },
        },
      ],
    }).compileComponents();
  });

  async function open(url: string) {
    const router = TestBed.inject(Router);
    await router.navigateByUrl(url);
    const fixture = TestBed.createComponent(CollectionPage);
    await fixture.whenStable();
    return {
      fixture,
      page: fixture.componentInstance,
      el: fixture.nativeElement as HTMLElement,
      router,
    };
  }

  it('leest filters, sortering en pagina uit de URL', async () => {
    const { el } = await open('/collectie?q=radio&type=CD&sort=artist,desc&page=2&size=48');

    expect(collectionApi.list).toHaveBeenCalledWith({
      q: 'radio',
      mediaType: 'CD',
      genre: null,
      sort: 'artist,desc',
      page: 2,
      size: 48,
    });
    expect(el.querySelectorAll('.grid li')).toHaveLength(2);
    expect(el.querySelector('h1')?.textContent).toContain('(2)');
  });

  it('negeert ongeldige waarden in de URL', async () => {
    await open('/collectie?type=LASERDISC&sort=user.id&size=7&page=-1');

    expect(collectionApi.list).toHaveBeenCalledWith({
      q: null,
      mediaType: null,
      genre: null,
      sort: 'added,desc',
      page: 0,
      size: 24,
    });
  });

  it('zet een filter in de URL en begint weer op pagina 1', async () => {
    const { fixture, page, router } = await open('/collectie?page=3');

    page.setMediaType('VINYL');
    await fixture.whenStable();

    expect(router.url).toBe('/collectie?type=VINYL');
    expect(collectionApi.list).toHaveBeenLastCalledWith(
      expect.objectContaining({ mediaType: 'VINYL', page: 0 }),
    );
  });

  it('selecteert de chip "Alle" zonder type-filter en wist het filter bij uitzetten', async () => {
    const { fixture, page, el, router } = await open('/collectie?type=CD');
    page.onTypeChip(undefined);
    await fixture.whenStable();

    expect(router.url).toBe('/collectie');
    const selected = el.querySelector(
      'mat-chip-option[aria-selected="true"], .mat-mdc-chip-selected',
    );
    expect(selected?.textContent).toContain('Alle');
  });

  it('filtert op genre via de URL en groepeert bij sorteren op genre', async () => {
    collectionApi.list.mockReturnValue(
      of(
        page([
          {
            ...item('1', 'Lateralus'),
            genreName: 'Hardrock - Metal',
            subgenreName: 'Progressive Metal',
          },
          {
            ...item('2', 'Aenima'),
            genreName: 'Hardrock - Metal',
            subgenreName: 'Progressive Metal',
          },
          { ...item('3', 'Iets'), genreName: null, subgenreName: null },
        ]),
      ),
    );
    const { el, router, fixture, page: pageCmp } = await open('/collectie?genre=g1&sort=genre,asc');

    expect(collectionApi.list).toHaveBeenLastCalledWith(
      expect.objectContaining({ genre: 'g1', sort: 'genre,asc' }),
    );
    expect(Array.from(el.querySelectorAll('h2.group')).map((h) => h.textContent?.trim())).toEqual([
      'Hardrock - Metal › Progressive Metal',
      'Zonder genre',
    ]);

    pageCmp.setGenre('none');
    await fixture.whenStable();
    expect(router.url).toBe('/collectie?genre=none&sort=genre,asc');
  });

  it('onthoudt de lijstweergave', async () => {
    const { fixture, page, el } = await open('/collectie');

    page.setView('list');
    await fixture.whenStable();

    expect(el.querySelectorAll('.list li')).toHaveLength(2);
    expect(el.querySelector('.list')?.textContent).toContain('Kast A');
    expect(localStorage.getItem('collection.view')).toBe('list');
  });

  it('toont een lege collectie anders dan een leeg zoekresultaat', async () => {
    collectionApi.list.mockReturnValue(of(page([])));
    const { el } = await open('/collectie');
    expect(el.textContent).toContain('Je collectie is nog leeg.');

    const filtered = await open('/collectie?q=xyz');
    expect(filtered.el.textContent).toContain('Niets gevonden met deze filters.');
  });
});

describe('groupByGenre', () => {
  it('voegt opeenvolgende items met hetzelfde genre samen', () => {
    const base = item('1', 'A');
    const groups = groupByGenre([
      { ...base, id: '1', genreName: 'Jazz', subgenreName: null },
      { ...base, id: '2', genreName: 'Jazz', subgenreName: null },
      { ...base, id: '3', genreName: 'Rock', subgenreName: 'Psychedelica' },
    ]);
    expect(groups.map((g) => [g.label, g.items.length])).toEqual([
      ['Jazz', 2],
      ['Rock › Psychedelica', 1],
    ]);
  });
});
