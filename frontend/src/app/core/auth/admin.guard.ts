import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { CurrentUserService } from './current-user.service';

/** Alleen voor de rol {@code admin}; anderen gaan naar de startpagina. Gebruik ná {@code authGuard}. */
export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(CurrentUserService)
    .hasRole('admin')
    .pipe(map((isAdmin) => isAdmin || router.createUrlTree(['/'])));
};
