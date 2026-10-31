import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

/** Stuurt niet-ingelogde gebruikers naar Keycloak en daarna terug naar de gevraagde pagina. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn()) {
    return true;
  }
  auth.login(state.url);
  return false;
};
