import { TestBed } from '@angular/core/testing';
import { GenreStore } from '../../../shared/genre-store';
import { Router, provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { CollectionApiService } from '../../../core/api/collection-api.service';
import { CollectionItem } from '../../../core/models/collection';
import { ManualReleasePage } from './manual-release-page';

const savedItem = {
  id: 'item-1',
  release: { artist: 'De Dijk', title: 'Nooit meer slapen' },
} as CollectionItem;

describe('ManualReleasePage', () => {
  const collectionApi = {
    add: vi.fn(() => of(savedItem)),
    get: vi.fn(),
    updateRelease: vi.fn(() => of(savedItem)),
    uploadCover: vi.fn(() => of(savedItem)),
    deleteCover: vi.fn(() => of(savedItem)),
  };
  const snackBar = { open: vi.fn() };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [ManualReleasePage],
      providers: [
        provideRouter([
          { path: 'handmatig', component: ManualReleasePage },
          { path: 'collectie/:id', children: [] },
        ]),
        { provide: CollectionApiService, useValue: collectionApi },
        {
          provide: GenreStore,
          useValue: {
            ensureLoaded: () => undefined,
            options: () => [],
            genres: () => [],
            matches: () => true,
          },
        },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();
  });

  async function open(url: string, id?: string) {
    await TestBed.inject(Router).navigateByUrl(url);
    const fixture = TestBed.createComponent(ManualReleasePage);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    await fixture.whenStable();
    return { fixture, page: fixture.componentInstance, el: fixture.nativeElement as HTMLElement };
  }

  it('neemt gegevens uit de scan-/zoek-flow over', async () => {
    const { page } = await open('/handmatig?barcode=8712345678906&artist=De%20Dijk&mediaType=CD');

    expect(page.form.getRawValue()).toMatchObject({
      barcode: '8712345678906',
      artist: 'De Dijk',
      title: '',
      mediaType: 'CD',
    });
  });

  it('stuurt een geldige release met tracks en eigen gegevens naar de backend', async () => {
    const { page } = await open('/handmatig');
    page.form.patchValue({
      artist: ' De Dijk ',
      title: 'Nooit meer slapen',
      mediaType: 'VINYL',
      releaseYear: '1985',
    });
    page.addTrack({ position: 'A1', title: 'Mag het licht uit', duration: '3:45' });
    page.addTrack({ position: 'C1', title: 'Zoals je bent' });
    page.onPositionChange(1);
    page.fields.patchValue({ storageLocation: 'Kast B' });

    page.save();

    expect(collectionApi.add).toHaveBeenCalledWith({
      release: expect.objectContaining({
        artist: 'De Dijk',
        releaseYear: 1985,
        mediaType: 'VINYL',
        barcode: null,
        tracks: [
          {
            discNumber: 1,
            position: 'A1',
            sortOrder: 1,
            title: 'Mag het licht uit',
            durationSeconds: 225,
            artist: null,
          },
          {
            discNumber: 2,
            position: 'C1',
            sortOrder: 2,
            title: 'Zoals je bent',
            durationSeconds: null,
            artist: null,
          },
        ],
      }),
      fields: expect.objectContaining({ storageLocation: 'Kast B' }),
    });
    expect(collectionApi.uploadCover).not.toHaveBeenCalled();
  });

  it('verstuurt niets zolang verplichte velden ontbreken', async () => {
    const { page } = await open('/handmatig');
    page.form.patchValue({ artist: 'De Dijk' });
    page.addTrack({ title: '' });

    page.save();

    expect(collectionApi.add).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Controleer de gemarkeerde velden.',
      'OK',
      expect.anything(),
    );
  });

  it('upload na het opslaan de gekozen cover', async () => {
    const { page } = await open('/handmatig');
    page.form.patchValue({ artist: 'De Dijk', title: 'X', mediaType: 'CD' });
    const blob = new Blob(['jpeg'], { type: 'image/jpeg' });
    page.coverBlob.set(blob);

    page.save();

    expect(collectionApi.uploadCover).toHaveBeenCalledWith('item-1', blob);
  });

  it('wijzigt tracks via verplaatsen en verwijderen', async () => {
    const { page } = await open('/handmatig');
    page.addTrack({ title: 'Een' });
    page.addTrack({ title: 'Twee' });
    page.addTrack({ title: 'Drie' });

    page.moveTrack(2, -1);
    page.removeTrack(0);

    expect(page.tracks.getRawValue().map((t) => t.title)).toEqual(['Drie', 'Twee']);
  });

  it('laadt in bewerkmodus de release en bewaart via updateRelease', async () => {
    collectionApi.get.mockReturnValue(
      of({
        id: 'item-1',
        release: {
          source: 'MANUAL',
          artist: 'De Dijk',
          title: 'Nooit meer slapen',
          releaseYear: 1985,
          mediaType: 'VINYL',
          formatDetails: null,
          label: null,
          catalogNumber: null,
          country: null,
          barcode: null,
          coverImageUrl: '/api/covers/abc',
          tracks: [
            {
              discNumber: 1,
              position: 'A1',
              sortOrder: 1,
              title: 'Mag het licht uit',
              durationSeconds: 225,
              artist: null,
            },
          ],
        },
      } as unknown as CollectionItem),
    );
    const { page } = await open('/handmatig', 'item-1');

    expect(page.form.controls.title.value).toBe('Nooit meer slapen');
    expect(page.tracks.at(0).controls.duration.value).toBe('3:45');

    page.clearCover();
    page.save();

    expect(collectionApi.updateRelease).toHaveBeenCalledWith(
      'item-1',
      expect.objectContaining({ releaseYear: 1985 }),
    );
    expect(collectionApi.deleteCover).toHaveBeenCalledWith('item-1');
  });

  it('weigert bewerken van een release uit Discogs', async () => {
    collectionApi.get.mockReturnValue(
      of({ id: 'item-2', release: { source: 'DISCOGS' } } as CollectionItem),
    );
    const { el } = await open('/handmatig', 'item-2');

    expect(el.textContent).toContain('gedeelde catalogus');
    expect(el.querySelector('form')).toBeNull();
  });
});
