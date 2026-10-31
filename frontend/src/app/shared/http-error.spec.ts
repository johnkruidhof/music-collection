import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage } from './http-error';

describe('errorMessage', () => {
  it('gebruikt het detail van een ProblemDetail', () => {
    const err = new HttpErrorResponse({ status: 400, error: { detail: 'Ongeldige barcode.' } });
    expect(errorMessage(err)).toBe('Ongeldige barcode.');
  });

  it('geeft een melding voor status 0 en 502', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('Geen verbinding');
    expect(errorMessage(new HttpErrorResponse({ status: 502 }))).toContain(
      'externe muziekdatabase',
    );
  });

  it('valt terug op een algemene melding', () => {
    expect(errorMessage(new Error('x'))).toContain('Er ging iets mis');
  });
});
