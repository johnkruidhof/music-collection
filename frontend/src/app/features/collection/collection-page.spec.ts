import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CollectionApiService } from '../../core/api/collection-api.service';
import { CollectionItemSummary, CollectionPage as PageDto } from '../../core/models/collection';
import { CollectionPage } from './collection-page';

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
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [CollectionPage],
      providers: [
        provideRouter([{ path: 'collectie', component: CollectionPage }]),
        { provide: CollectionApiService, useValue: collectionApi },
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
