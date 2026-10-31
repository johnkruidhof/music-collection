import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { CollectionApiService } from '../../core/api/collection-api.service';
import { CollectionItem } from '../../core/models/collection';
import { Release, Track } from '../../core/models/release';
import { CollectionItemPage, groupByDisc, sourceUrl } from './collection-item-page';

const track = (
  discNumber: number,
  sortOrder: number,
  title: string,
  durationSeconds: number | null = 200,
): Track => ({
  discNumber,
  position: String(sortOrder),
  sortOrder,
  title,
  durationSeconds,
  artist: null,
});

const release = (overrides: Partial<Release> = {}): Release => ({
  id: 'r1',
  barcode: '724385522925',
  artist: 'Radiohead',
  title: 'OK Computer',
  releaseYear: 1997,
  mediaType: 'VINYL',
  formatDetails: '2×Vinyl, LP, Album',
  label: 'Parlophone',
  catalogNumber: 'NODATA 02',
  country: 'UK',
  coverImageUrl: null,
  coverThumbnailUrl: null,
  source: 'DISCOGS',
  discogsId: '2149389',
  musicbrainzId: null,
  tracks: [
    track(1, 1, 'Airbag', 284),
    track(1, 2, 'Paranoid Android', 383),
    track(2, 3, 'Karma Police', 261),
  ],
  ...overrides,
});

const item = (overrides: Partial<Release> = {}): CollectionItem => ({
  id: 'item-1',
  release: release(overrides),
  conditionMedia: 'NEAR_MINT',
  conditionSleeve: null,
  purchaseDate: null,
  purchasePrice: 24.99,
  purchaseLocation: null,
  storageLocation: 'Kast A',
  notes: null,
  addedAt: '2026-10-31T12:00:00Z',
  updatedAt: '2026-10-31T12:00:00Z',
});

describe('groupByDisc / sourceUrl', () => {
  it('groepeert tracks per disc in volgorde', () => {
    const discs = groupByDisc([track(2, 3, 'C'), track(1, 2, 'B'), track(1, 1, 'A')]);
    expect(discs.map((d) => [d.number, d.tracks.map((t) => t.title)])).toEqual([
      [1, ['A', 'B']],
      [2, ['C']],
    ]);
  });

  it('linkt naar Discogs of MusicBrainz, niet bij handmatig', () => {
    expect(sourceUrl(release())).toBe('https://www.discogs.com/release/2149389');
    expect(sourceUrl(release({ source: 'MUSICBRAINZ', musicbrainzId: 'abc' }))).toBe(
      'https://musicbrainz.org/release/abc',
    );
    expect(sourceUrl(release({ source: 'MANUAL' }))).toBeNull();
  });
});

describe('CollectionItemPage', () => {
  const collectionApi = {
    get: vi.fn(() => of(item())),
    update: vi.fn(() => of(item())),
    delete: vi.fn(() => of(undefined)),
  };
  const snackBar = { open: vi.fn() };
  let dialogResult: boolean;
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(dialogResult) })) };

  beforeEach(async () => {
    vi.clearAllMocks();
    collectionApi.get.mockReturnValue(of(item()));
    await TestBed.configureTestingModule({
      imports: [CollectionItemPage],
      providers: [
        provideRouter([{ path: 'collectie', children: [] }]),
        { provide: CollectionApiService, useValue: collectionApi },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
      ],
    }).compileComponents();
  });

  async function open() {
    const fixture = TestBed.createComponent(CollectionItemPage);
    fixture.componentRef.setInput('id', 'item-1');
    await fixture.whenStable();
    return { fixture, page: fixture.componentInstance, el: fixture.nativeElement as HTMLElement };
  }

  it('toont de release met tracklist per disc en totale duur', async () => {
    const { el } = await open();

    expect(collectionApi.get).toHaveBeenCalledWith('item-1');
    expect(el.querySelector('h1')?.textContent).toContain('OK Computer');
    expect(Array.from(el.querySelectorAll('h3')).map((h) => h.textContent?.trim())).toEqual([
      'Disc 1',
      'Disc 2',
    ]);
    expect(el.querySelector('.total')?.textContent).toContain('15:28');
    expect(el.querySelector('a[href="https://www.discogs.com/release/2149389"]')).not.toBeNull();
    // Releases uit Discogs zijn niet te bewerken.
    expect(el.textContent).not.toContain('Bewerken');
  });

  it('vult de eigen velden en slaat wijzigingen op', async () => {
    const { page } = await open();
    expect(page.fields.controls.storageLocation.value).toBe('Kast A');
    expect(page.fields.controls.purchasePrice.value).toBe('24,99');

    page.fields.controls.storageLocation.setValue('Kast B');
    page.fields.markAsDirty();
    page.saveFields();

    expect(collectionApi.update).toHaveBeenCalledWith(
      'item-1',
      expect.objectContaining({
        storageLocation: 'Kast B',
        purchasePrice: 24.99,
        conditionMedia: 'NEAR_MINT',
      }),
    );
    expect(page.fields.pristine).toBe(true);
  });

  it('slaat op via de knop zonder de pagina te verlaten', async () => {
    const { fixture, page, el } = await open();
    page.fields.controls.notes.setValue('Eerste persing');
    page.fields.markAsDirty();
    await fixture.whenStable();

    const button = Array.from(el.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Opslaan'),
    );
    button?.click();

    expect(collectionApi.update).toHaveBeenCalledWith(
      'item-1',
      expect.objectContaining({ notes: 'Eerste persing' }),
    );
  });

  it('toont "Bewerken" bij een handmatige release', async () => {
    collectionApi.get.mockReturnValue(of(item({ source: 'MANUAL', discogsId: null })));
    const { el } = await open();

    expect(el.querySelector('a[href="/collectie/item-1/bewerken"]')).not.toBeNull();
  });

  it('verwijdert pas na bevestiging', async () => {
    const { page } = await open();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate');

    dialogResult = false;
    page.confirmDelete();
    expect(collectionApi.delete).not.toHaveBeenCalled();

    dialogResult = true;
    page.confirmDelete();
    expect(collectionApi.delete).toHaveBeenCalledWith('item-1');
    expect(navigate).toHaveBeenCalledWith(['/collectie']);
  });
});
