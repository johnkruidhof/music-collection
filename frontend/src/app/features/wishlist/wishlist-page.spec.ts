import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { WishlistApiService } from '../../core/api/wishlist-api.service';
import { CollectionItem } from '../../core/models/collection';
import { WishlistItem } from '../../core/models/wishlist';
import { WishlistPage } from './wishlist-page';

const wish = (id: string, title: string): WishlistItem => ({
  id,
  releaseId: 'r-' + id,
  artist: 'Radiohead',
  title,
  releaseYear: 2000,
  mediaType: 'VINYL',
  formatDetails: '2×Vinyl',
  label: 'Parlophone',
  country: 'UK',
  coverThumbnailUrl: null,
  source: 'DISCOGS',
  discogsId: '1',
  musicbrainzId: null,
  notes: 'cadeau',
  addedAt: '2026-10-31T12:00:00Z',
});

describe('WishlistPage', () => {
  const wishlistApi = {
    list: vi.fn(() => of([wish('1', 'Kid A'), wish('2', 'Amnesiac')])),
    acquire: vi.fn(() => of({ id: 'item-1' } as CollectionItem)),
    delete: vi.fn(() => of(undefined)),
  };
  let confirmed = true;
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(confirmed) })) };
  const snackBar = { open: vi.fn(() => ({ onAction: () => of() })) };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [WishlistPage],
      providers: [
        provideRouter([]),
        { provide: WishlistApiService, useValue: wishlistApi },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();
  });

  it('toont de wensen en verplaatst een gekochte naar de collectie', async () => {
    const fixture = TestBed.createComponent(WishlistPage);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.item')).toHaveLength(2);
    expect(el.textContent).toContain('cadeau');

    fixture.componentInstance.acquire(wish('1', 'Kid A'));
    await fixture.whenStable();

    expect(wishlistApi.acquire).toHaveBeenCalledWith('1');
    expect(el.querySelectorAll('.item')).toHaveLength(1);
    expect(snackBar.open).toHaveBeenCalledWith(
      'In je collectie: Radiohead – Kid A',
      'Bekijken',
      expect.anything(),
    );
  });

  it('haalt pas weg na bevestiging', async () => {
    const page = TestBed.createComponent(WishlistPage).componentInstance;

    confirmed = false;
    page.remove(wish('2', 'Amnesiac'));
    expect(wishlistApi.delete).not.toHaveBeenCalled();

    confirmed = true;
    page.remove(wish('2', 'Amnesiac'));
    expect(wishlistApi.delete).toHaveBeenCalledWith('2');
    expect(page.items()?.map((i) => i.title)).toEqual(['Kid A']);
  });
});
