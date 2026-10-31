import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { AdminApiService } from '../../core/api/admin-api.service';
import { AdminRelease, AdminReleasePage } from '../../core/models/admin';
import { AdminPage } from './admin-page';

const release = (id: string, collectionCount: number): AdminRelease => ({
  id,
  artist: 'Radiohead',
  title: 'OK Computer ' + id,
  releaseYear: 1997,
  mediaType: 'CD',
  formatDetails: null,
  label: null,
  catalogNumber: null,
  country: 'UK',
  barcode: null,
  source: 'DISCOGS',
  discogsId: id,
  musicbrainzId: null,
  coverThumbnailUrl: null,
  collectionCount,
  wishlistCount: 0,
  createdAt: '2026-10-31T12:00:00Z',
});

const page: AdminReleasePage = {
  content: [release('1', 2), release('2', 0)],
  page: 0,
  size: 25,
  totalElements: 2,
  totalPages: 1,
  unusedCount: 1,
};

describe('AdminPage', () => {
  const adminApi = {
    search: vi.fn(() => of(page)),
    merge: vi.fn(() => of(release('1', 3))),
    delete: vi.fn(() => of(undefined)),
    deleteUnused: vi.fn(() => of({ deleted: 1 })),
  };
  let dialogResult: unknown;
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(dialogResult) })) };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [AdminPage],
      providers: [
        { provide: AdminApiService, useValue: adminApi },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('toont het gebruik en biedt verwijderen alleen bij ongebruikte releases', async () => {
    const fixture = TestBed.createComponent(AdminPage);
    await fixture.whenStable();
    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('.row');

    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('2 in collecties');
    expect(rows[0].querySelector('.delete')).toBeNull();
    expect(rows[1].querySelector('.delete')).not.toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('1 ongebruikte opruimen');
  });

  it('voegt samen met het gekozen doel', () => {
    const admin = TestBed.createComponent(AdminPage).componentInstance;
    dialogResult = release('1', 2);

    admin.merge(release('2', 0));

    expect(adminApi.merge).toHaveBeenCalledWith('2', '1');
    expect(adminApi.search).toHaveBeenCalledTimes(2);
  });

  it('ruimt pas op na bevestiging', () => {
    const admin = TestBed.createComponent(AdminPage).componentInstance;

    dialogResult = false;
    admin.deleteUnused(1);
    expect(adminApi.deleteUnused).not.toHaveBeenCalled();

    dialogResult = true;
    admin.deleteUnused(1);
    expect(adminApi.deleteUnused).toHaveBeenCalled();
  });
});
