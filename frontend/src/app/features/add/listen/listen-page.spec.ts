import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { CollectionApiService } from '../../../core/api/collection-api.service';
import { RecognitionApiService } from '../../../core/api/recognition-api.service';
import { WishlistApiService } from '../../../core/api/wishlist-api.service';
import { RecognitionResult } from '../../../core/models/recognition';
import { AudioRecorderService } from '../../../shared/audio-recorder.service';
import { ListenPage } from './listen-page';

const result: RecognitionResult = {
  track: {
    artist: 'Radiohead',
    title: 'Karma Police',
    album: 'OK Computer',
    releaseYear: 1997,
    label: null,
    songLink: 'https://lis.tn/KarmaPolice',
  },
  inCollection: [
    {
      id: 'item-1',
      releaseId: 'r1',
      artist: 'Radiohead',
      title: 'OK Computer',
      releaseYear: 1997,
      mediaType: 'VINYL',
      formatDetails: null,
      coverThumbnailUrl: null,
      conditionMedia: null,
      storageLocation: 'Kast A',
      addedAt: '2026-10-31T12:00:00Z',
    },
  ],
  candidates: [],
};

describe('ListenPage', () => {
  const recognitionApi = { enabled: vi.fn(() => of(true)), recognize: vi.fn(() => of(result)) };
  const recorder = {
    supported: vi.fn(() => true),
    record: vi.fn(async () => ({
      blob: new Blob(['x'], { type: 'audio/webm' }),
      fileName: 'opname.webm',
    })),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [ListenPage],
      providers: [
        provideRouter([]),
        { provide: RecognitionApiService, useValue: recognitionApi },
        { provide: AudioRecorderService, useValue: recorder },
        { provide: CollectionApiService, useValue: { addCandidate: vi.fn() } },
        { provide: WishlistApiService, useValue: { addCandidate: vi.fn() } },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('neemt op, herkent en toont het nummer en wat al in de collectie staat', async () => {
    const fixture = TestBed.createComponent(ListenPage);
    await fixture.componentInstance.listen();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(recorder.record).toHaveBeenCalledWith(10_000, expect.any(Function));
    expect(recognitionApi.recognize).toHaveBeenCalledWith(expect.any(Blob), 'opname.webm');
    expect(el.querySelector('.track-title')?.textContent).toContain('Karma Police');
    expect(el.textContent).toContain('Staat al in je collectie');
    expect(el.querySelector('.owned a')?.getAttribute('href')).toBe('/collectie/item-1');
    expect(el.textContent).toContain('Kast A');
  });

  it('meldt als niets herkend is', async () => {
    recognitionApi.recognize.mockReturnValueOnce(
      of({ track: null, inCollection: [], candidates: [] }),
    );
    const fixture = TestBed.createComponent(ListenPage);

    await fixture.componentInstance.listen();
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Niet herkend');
  });

  it('meldt een geweigerde microfoon', async () => {
    recorder.record.mockRejectedValueOnce(
      Object.assign(new Error('x'), { name: 'NotAllowedError' }),
    );
    const fixture = TestBed.createComponent(ListenPage);

    await fixture.componentInstance.listen();
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Geen toegang tot de microfoon',
    );
    expect(recognitionApi.recognize).not.toHaveBeenCalled();
  });

  it('toont de melding van de backend', async () => {
    recognitionApi.recognize.mockReturnValueOnce(
      throwError(
        () =>
          new HttpErrorResponse({ status: 503, error: { detail: 'Luisteren is niet ingesteld.' } }),
      ),
    );
    const fixture = TestBed.createComponent(ListenPage);

    await fixture.componentInstance.listen();
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Luisteren is niet ingesteld.',
    );
  });

  it('legt uit hoe je luisteren instelt als er geen sleutel is', async () => {
    recognitionApi.enabled.mockReturnValueOnce(of(false));
    const fixture = TestBed.createComponent(ListenPage);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('AUDD_API_TOKEN');
    expect(el.querySelector('.listen')).toBeNull();
  });
});
