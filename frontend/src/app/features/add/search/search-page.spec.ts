import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { CollectionApiService } from '../../../core/api/collection-api.service';
import { LookupApiService } from '../../../core/api/lookup-api.service';
import { CollectionItem } from '../../../core/models/collection';
import { Release, ReleaseCandidate } from '../../../core/models/release';
import { SearchPage } from './search-page';

const candidate: ReleaseCandidate = {
  source: 'MUSICBRAINZ',
  externalId: '6a09041b-0f79-3278-88d9-0552dd28d37f',
  artist: 'Radiohead',
  title: 'OK Computer',
  releaseYear: 1997,
  mediaType: 'CD',
  formatDetails: 'CD',
  label: 'Capitol Records',
  catalogNumber: null,
  country: 'US',
  barcode: '724385522925',
  coverThumbnailUrl: null,
};

describe('SearchPage', () => {
  const lookupApi = {
    search: vi.fn(() => of([candidate])),
    details: vi.fn(() =>
      of({
        tracks: [
          {
            discNumber: 1,
            position: '1',
            sortOrder: 1,
            title: 'Airbag',
            durationSeconds: 284,
            artist: null,
          },
        ],
      } as unknown as Release),
    ),
  };
  const collectionApi = {
    addCandidate: vi.fn(() =>
      of({
        id: 'item-1',
        release: { artist: 'Radiohead', title: 'OK Computer' },
      } as CollectionItem),
    ),
  };
  const snackBar = { open: vi.fn(() => ({ onAction: () => of() })) };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [SearchPage],
      providers: [
        provideRouter([{ path: 'zoeken', component: SearchPage }]),
        { provide: LookupApiService, useValue: lookupApi },
        { provide: CollectionApiService, useValue: collectionApi },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();
  });

  async function open(url: string) {
    await TestBed.inject(Router).navigateByUrl(url);
    const fixture = TestBed.createComponent(SearchPage);
    await fixture.whenStable();
    return { fixture, page: fixture.componentInstance, el: fixture.nativeElement as HTMLElement };
  }

  it('zoekt op basis van de query-params en toont de resultaten', async () => {
    const { el } = await open('/zoeken?artist=Radiohead&year=1997&mediaType=CD');

    expect(lookupApi.search).toHaveBeenCalledWith({
      artist: 'Radiohead',
      title: null,
      year: 1997,
      mediaType: 'CD',
    });
    expect(el.textContent).toContain('1 resultaten');
    expect(el.querySelectorAll('app-candidate-card')).toHaveLength(1);
  });

  it('zoekt niet zonder artiest of titel en meldt dat', async () => {
    const { fixture, page, el } = await open('/zoeken?barcode=0000000000000');

    page.submit();
    await fixture.whenStable();

    expect(lookupApi.search).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Barcode 0000000000000 is niet gevonden');
    expect(el.textContent).toContain('Vul minstens een artiest of een titel in.');
  });

  it('markeert een toegevoegde kandidaat', async () => {
    const { page } = await open('/zoeken?title=OK%20Computer');

    page.add(candidate);

    expect(collectionApi.addCandidate).toHaveBeenCalledWith(candidate);
    expect(page.added().has(`MUSICBRAINZ:${candidate.externalId}`)).toBe(true);
  });

  it('klapt de tracks van een kandidaat open en dicht', async () => {
    const { fixture, page, el } = await open('/zoeken?title=OK%20Computer');

    page.toggleTracks(candidate);
    await fixture.whenStable();
    expect(lookupApi.details).toHaveBeenCalledWith('MUSICBRAINZ', candidate.externalId);
    expect(el.querySelector('.tracks')?.textContent).toContain('Airbag');
    expect(el.querySelector('.tracks')?.textContent).toContain('4:44');

    page.toggleTracks(candidate);
    await fixture.whenStable();
    expect(el.querySelector('.tracks')).toBeNull();
  });
});
