import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { GenreApiService } from '../../core/api/genre-api.service';
import { Genre } from '../../core/models/genre';
import { GenresPage } from './genres-page';

const genres: Genre[] = [
  {
    id: 'metal',
    name: 'Hardrock - Metal',
    parentId: null,
    itemCount: 1,
    subgenres: [{ id: 'doom', name: 'Doom Metal', parentId: 'metal', itemCount: 2, subgenres: [] }],
  },
  { id: 'jazz', name: 'Jazz', parentId: null, itemCount: 0, subgenres: [] },
];

describe('GenresPage', () => {
  const genreApi = {
    list: vi.fn(() => of(genres)),
    create: vi.fn(() => of(genres[1])),
    rename: vi.fn(() => of(genres[1])),
    delete: vi.fn(() => of(undefined)),
    importTree: vi.fn(() =>
      of({
        genresCreated: 1,
        subgenresCreated: 2,
        artistsLinked: 614,
        itemsAssigned: 5,
        wishesAssigned: 1,
      }),
    ),
  };
  let confirmed = true;
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(confirmed) })) };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [GenresPage],
      providers: [
        provideRouter([]),
        { provide: GenreApiService, useValue: genreApi },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('toont genres met subgenres en het totaal aantal releases', async () => {
    const fixture = TestBed.createComponent(GenresPage);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelectorAll('.tree > li')).toHaveLength(2);
    expect(el.querySelector('.subgenres')?.textContent).toContain('Doom Metal');
    // 1 in het genre zelf + 2 in het subgenre
    expect(el.querySelector('.tree > li .count')?.textContent?.trim()).toBe('3');
  });

  it('maakt een subgenre aan onder het gekozen genre', async () => {
    const fixture = TestBed.createComponent(GenresPage);
    const page = fixture.componentInstance;
    await fixture.whenStable();

    page.startNew('metal');
    (page as unknown as { nameControl: { setValue(v: string): void } }).nameControl.setValue(
      ' Sludge ',
    );
    page.save();

    expect(genreApi.create).toHaveBeenCalledWith('Sludge', 'metal');
    expect(page.editing()).toBeNull();
  });

  it('verwijdert pas na bevestiging', () => {
    const page = TestBed.createComponent(GenresPage).componentInstance;

    confirmed = false;
    page.remove(genres[0]);
    expect(genreApi.delete).not.toHaveBeenCalled();

    confirmed = true;
    page.remove(genres[0]);
    expect(genreApi.delete).toHaveBeenCalledWith('metal');
  });

  it('importeert een boomstructuur en toont het resultaat', async () => {
    const fixture = TestBed.createComponent(GenresPage);
    fixture.componentInstance.importTree(new File(['|-- Rock'], 'boomstructuur.txt'));
    await fixture.whenStable();

    expect(genreApi.importTree).toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).querySelector('.result')?.textContent).toContain(
      '614',
    );
  });
});
