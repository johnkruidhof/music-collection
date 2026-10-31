import { registerLocaleData } from '@angular/common';
import localeNl from '@angular/common/locales/nl';
import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { authInterceptor } from './core/auth/auth.interceptor';
import { DutchPaginatorIntl } from './shared/dutch-paginator-intl';

registerLocaleData(localeNl);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideOAuthClient(),
    provideAppInitializer(() => inject(AuthService).init()),
    // Nederlandse datums/getallen (DatePipe e.d.) en paginator-teksten.
    { provide: LOCALE_ID, useValue: 'nl' },
    { provide: MatPaginatorIntl, useClass: DutchPaginatorIntl },
  ],
};
