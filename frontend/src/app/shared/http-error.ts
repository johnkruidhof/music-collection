import { HttpErrorResponse } from '@angular/common/http';

/** Leesbare (Nederlandse) melding uit een HTTP-fout; gebruikt het `detail` van een ProblemDetail. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Geen verbinding met de server.';
    }
    const detail = (error.error as { detail?: unknown } | null)?.detail;
    if (typeof detail === 'string' && detail.trim() !== '') {
      return detail;
    }
    if (error.status === 401) {
      return 'Je sessie is verlopen; log opnieuw in.';
    }
    if (error.status === 404) {
      return 'Niet gevonden.';
    }
    if (error.status === 502) {
      return 'De externe muziekdatabase is nu niet bereikbaar. Probeer het later opnieuw.';
    }
  }
  return 'Er ging iets mis. Probeer het opnieuw.';
}
