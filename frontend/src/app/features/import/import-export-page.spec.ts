import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { CollectionApiService } from '../../core/api/collection-api.service';
import { ImportJob } from '../../core/models/import-job';
import { ImportExportPage, POLL_INTERVAL_MS } from './import-export-page';

const job = (overrides: Partial<ImportJob>): ImportJob => ({
  id: 'job-1',
  status: 'RUNNING',
  total: 10,
  processed: 0,
  imported: 0,
  skipped: 0,
  failed: 0,
  errors: [],
  startedAt: '2026-10-31T12:00:00Z',
  finishedAt: null,
  ...overrides,
});

describe('ImportExportPage', () => {
  const collectionApi = {
    latestImport: vi.fn(() => of<ImportJob | null>(null)),
    importDiscogs: vi.fn(() => of(job({}))),
    importStatus: vi.fn(),
    exportCsv: vi.fn(() =>
      of({ blob: new Blob(['a;b']), fileName: 'muziekcollectie-2026-10-31.csv' }),
    ),
  };

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [ImportExportPage],
      providers: [
        provideRouter([]),
        { provide: CollectionApiService, useValue: collectionApi },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();
  });

  afterEach(() => vi.useRealTimers());

  it('volgt de voortgang tot de import klaar is', () => {
    collectionApi.importStatus
      .mockReturnValueOnce(of(job({ processed: 5, imported: 4, failed: 1 })))
      .mockReturnValueOnce(
        of(
          job({
            status: 'FINISHED',
            processed: 10,
            imported: 8,
            skipped: 1,
            failed: 1,
            errors: ['regel 4: niet gevonden'],
          }),
        ),
      );
    const fixture = TestBed.createComponent(ImportExportPage);
    const page = fixture.componentInstance;

    page.start(new File(['x'], 'export.csv'));
    expect(page.job()?.status).toBe('RUNNING');

    vi.advanceTimersByTime(POLL_INTERVAL_MS);
    expect(page.job()?.processed).toBe(5);

    vi.advanceTimersByTime(POLL_INTERVAL_MS);
    expect(page.job()?.status).toBe('FINISHED');

    vi.advanceTimersByTime(POLL_INTERVAL_MS * 3);
    expect(collectionApi.importStatus).toHaveBeenCalledTimes(2);
  });

  it('pakt een lopende import weer op bij openen', () => {
    collectionApi.latestImport.mockReturnValueOnce(of(job({ processed: 3 })));
    collectionApi.importStatus.mockReturnValue(of(job({ status: 'FINISHED', processed: 10 })));

    const page = TestBed.createComponent(ImportExportPage).componentInstance;
    expect(page.job()?.processed).toBe(3);

    vi.advanceTimersByTime(POLL_INTERVAL_MS);
    expect(page.job()?.status).toBe('FINISHED');
  });

  it('downloadt de export met de bestandsnaam van de backend', () => {
    const createObjectURL = vi.fn(() => 'blob:x');
    URL.createObjectURL = createObjectURL;
    URL.revokeObjectURL = vi.fn();
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);

    TestBed.createComponent(ImportExportPage).componentInstance.export();

    expect(createObjectURL).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
    expect((click.mock.contexts[0] as HTMLAnchorElement).download).toBe(
      'muziekcollectie-2026-10-31.csv',
    );
  });
});
