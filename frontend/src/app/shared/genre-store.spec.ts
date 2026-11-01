import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { GenreApiService } from '../core/api/genre-api.service';
import { GenreStore, genreLabel } from './genre-store';

describe('GenreStore', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: GenreApiService,
          useValue: {
            list: () =>
              of([
                {
                  id: 'metal',
                  name: 'Hardrock - Metal',
                  parentId: null,
                  itemCount: 0,
                  subgenres: [
                    {
                      id: 'doom',
                      name: 'Doom Metal',
                      parentId: 'metal',
                      itemCount: 0,
                      subgenres: [],
                    },
                  ],
                },
              ]),
          },
        },
      ],
    });
  });

  it('maakt een platte keuzelijst en filtert genres inclusief subgenres', () => {
    const store = TestBed.inject(GenreStore);
    store.reload().subscribe();

    expect(store.options().map((o) => o.label)).toEqual([
      'Hardrock - Metal',
      'Hardrock - Metal › Doom Metal',
    ]);
    expect(store.matches('metal', 'doom')).toBe(true);
    expect(store.matches('doom', 'metal')).toBe(false);
    expect(store.matches('none', null)).toBe(true);
    expect(store.matches(null, 'x')).toBe(true);
  });

  it('maakt leesbare labels', () => {
    expect(genreLabel('Jazz', null)).toBe('Jazz');
    expect(genreLabel(null, null)).toBe('Zonder genre');
  });
});
