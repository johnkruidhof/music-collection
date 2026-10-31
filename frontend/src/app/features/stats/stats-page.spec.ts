import { registerLocaleData } from '@angular/common';
import localeNl from '@angular/common/locales/nl';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CollectionApiService } from '../../core/api/collection-api.service';
import { CollectionStats } from '../../core/models/stats';
import { StatsPage } from './stats-page';

registerLocaleData(localeNl);

const stats: CollectionStats = {
  totalItems: 4,
  totalValue: 42.49,
  itemsWithPrice: 2,
  byMediaType: [
    { mediaType: 'VINYL', count: 3 },
    { mediaType: 'BLU_RAY', count: 1 },
  ],
  byDecade: [
    { decade: 1990, count: 3 },
    { decade: null, count: 1 },
  ],
  topArtists: [{ artist: 'Radiohead', count: 2 }],
  byCondition: [
    { condition: 'NEAR_MINT', count: 2 },
    { condition: null, count: 2 },
  ],
};

describe('StatsPage', () => {
  const collectionApi = { stats: vi.fn(() => of(stats)) };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatsPage],
      providers: [provideRouter([]), { provide: CollectionApiService, useValue: collectionApi }],
    }).compileComponents();
  });

  it('toont kerncijfers en verdelingen met Nederlandse labels', async () => {
    const fixture = TestBed.createComponent(StatsPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toMatch(/€\s42/);
    expect(text).toContain('2 van 4 met aankoopprijs');
    expect(text).toMatch(/€\s21,25/);
    expect(text).toContain('Blu-ray');
    expect(text).toContain('1990–1999');
    expect(text).toContain('Jaar onbekend');
    expect(text).toContain('Near Mint (NM)');
    expect(text).toContain('Niet ingevuld');
  });

  it('schaalt de langste staaf naar 85% van de rij', async () => {
    const fixture = TestBed.createComponent(StatsPage);
    await fixture.whenStable();
    const bars = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLElement>(
        'app-bar-list:first-of-type .bar',
      ),
    ).map((bar) => bar.style.width);

    expect(bars.slice(0, 2)).toEqual(['85%', expect.stringMatching(/^28\.33/)]);
  });
});
