import { Component, input, output } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, Subject, of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { CollectionApiService } from '../../../core/api/collection-api.service';
import { LookupApiService } from '../../../core/api/lookup-api.service';
import { CollectionItem } from '../../../core/models/collection';
import { ReleaseCandidate } from '../../../core/models/release';
import { BarcodeScanner } from './barcode-scanner';
import { ScanPage } from './scan-page';

@Component({ selector: 'app-barcode-scanner', template: '' })
class FakeScanner {
  readonly active = input(true);
  readonly scanned = output<string>();
}

const candidate = (externalId: string): ReleaseCandidate => ({
  source: 'DISCOGS',
  externalId,
  artist: 'Radiohead',
  title: 'OK Computer',
  releaseYear: 1997,
  mediaType: 'VINYL',
  formatDetails: 'Vinyl, LP, Album',
  label: 'Parlophone',
  catalogNumber: 'NODATA 02',
  country: 'UK',
  barcode: '724385522925',
  coverThumbnailUrl: null,
});

describe('ScanPage', () => {
  let lookupResult: Observable<ReleaseCandidate[]>;
  let addResult: Subject<CollectionItem>;
  const lookupApi = { byBarcode: vi.fn(() => lookupResult) };
  const collectionApi = { addCandidate: vi.fn(() => addResult) };
  const snackBar = { open: vi.fn(() => ({ onAction: () => of() })) };

  beforeEach(async () => {
    vi.clearAllMocks();
    addResult = new Subject<CollectionItem>();
    await TestBed.configureTestingModule({
      imports: [ScanPage],
      providers: [
        provideRouter([]),
        { provide: LookupApiService, useValue: lookupApi },
        { provide: CollectionApiService, useValue: collectionApi },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    })
      .overrideComponent(ScanPage, {
        remove: { imports: [BarcodeScanner] },
        add: { imports: [FakeScanner] },
      })
      .compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(ScanPage);
    return { fixture, page: fixture.componentInstance, el: fixture.nativeElement as HTMLElement };
  }

  it('toont één gevonden release als bevestigingskaart', async () => {
    lookupResult = of([candidate('1')]);
    const { fixture, page, el } = create();

    page.onScanned('724385522925');
    await fixture.whenStable();

    expect(lookupApi.byBarcode).toHaveBeenCalledWith('724385522925');
    expect(el.querySelector('h2')?.textContent).toContain('Gevonden');
    expect(el.querySelectorAll('app-candidate-card')).toHaveLength(1);
  });

  it('negeert herhaalde scans zolang er een resultaat open staat', () => {
    lookupResult = of([candidate('1'), candidate('2')]);
    const { page } = create();

    page.onScanned('724385522925');
    page.onScanned('724385522925');

    expect(lookupApi.byBarcode).toHaveBeenCalledTimes(1);
    expect(page.state().kind).toBe('candidates');
  });

  it('biedt zoeken en handmatig invoeren aan als niets gevonden is', async () => {
    lookupResult = of([]);
    const { fixture, page, el } = create();

    page.onScanned('0000000000000');
    await fixture.whenStable();

    const links = Array.from(el.querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(links).toContain('/zoeken?barcode=0000000000000');
    expect(links).toContain('/handmatig?barcode=0000000000000');
  });

  it('toont de foutmelding van de backend', async () => {
    lookupResult = throwError(
      () =>
        new HttpErrorResponse({ status: 502, error: { detail: 'Discogs is niet bereikbaar.' } }),
    );
    const { fixture, page, el } = create();

    page.onScanned('724385522925');
    await fixture.whenStable();

    expect(el.textContent).toContain('Discogs is niet bereikbaar.');
  });

  it('gaat na toevoegen direct terug naar scannen (batch-modus)', () => {
    lookupResult = of([candidate('1')]);
    const { page } = create();
    page.onScanned('724385522925');

    page.add(candidate('1'));
    expect(page.adding()).not.toBeNull();
    addResult.next({
      id: 'item-1',
      release: { artist: 'Radiohead', title: 'OK Computer' },
    } as CollectionItem);

    expect(collectionApi.addCandidate).toHaveBeenCalledWith(candidate('1'));
    expect(page.state().kind).toBe('scanning');
    expect(page.addedCount()).toBe(1);
    expect(snackBar.open).toHaveBeenCalledWith(
      'Toegevoegd: Radiohead – OK Computer',
      'Bekijken',
      expect.anything(),
    );
  });

  it('valideert handmatig ingevoerde barcodes', () => {
    const { page } = create();
    const form = page as unknown as { manualBarcode: { setValue(v: string): void } };

    form.manualBarcode.setValue('123');
    page.lookupManual();
    expect(lookupApi.byBarcode).not.toHaveBeenCalled();

    lookupResult = of([]);
    form.manualBarcode.setValue('7 24385-52292 5');
    page.lookupManual();
    expect(lookupApi.byBarcode).toHaveBeenCalledWith('724385522925');
  });
});
