import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Stuurt niet-ingelogde gebruikers naar Keycloak en daarna terug naar de gevraagde pagina. Offline met een
 * eerdere sessie (PWA) mag de gecachte collectie bekeken worden; Keycloak is dan toch niet bereikbaar.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.hasSession()) {
    return true;
  }
  auth.login(state.url);
  return false;
};
