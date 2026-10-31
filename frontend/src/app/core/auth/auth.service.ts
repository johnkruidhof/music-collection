import { Injectable, computed, inject, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

interface IdentityClaims {
  preferred_username?: string;
  name?: string;
  email?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauth = inject(OAuthService);

  private readonly loggedIn = signal(false);
  private readonly claims = signal<IdentityClaims | null>(null);

  readonly isLoggedIn = this.loggedIn.asReadonly();
  readonly displayName = computed(() => {
    const c = this.claims();
    return c?.name || c?.preferred_username || null;
  });

  /** Wordt bij het opstarten van de app aangeroepen (zie app.config.ts). */
  async init(): Promise<void> {
    this.oauth.configure(authConfig());
    this.oauth.setupAutomaticSilentRefresh();
    this.oauth.events.subscribe(() => this.refreshState());

    try {
      await this.oauth.loadDiscoveryDocumentAndTryLogin();
    } catch (err) {
      console.error('Keycloak niet bereikbaar', err);
    }
    this.refreshState();

    // Na login terug naar de pagina waar de gebruiker vandaan kwam. Dit draait vóór de eerste
    // router-navigatie (app-initializer); die navigeert naar de browser-URL, dus die zetten we hier.
    const target = this.loggedIn() ? safeReturnUrl(this.oauth.state) : null;
    if (target) {
      window.history.replaceState(window.history.state, '', target);
    }
  }

  login(targetUrl = ''): void {
    // Niet zelf coderen: de bibliotheek doet encodeURIComponent op de state (zie safeReturnUrl).
    this.oauth.initCodeFlow(targetUrl);
  }

  /** Opent direct het registratieformulier van Keycloak (self-registration). */
  register(): void {
    this.oauth.initCodeFlow('', { prompt: 'create' });
  }

  logout(): void {
    this.oauth.logOut();
  }

  accessToken(): string | null {
    return this.oauth.hasValidAccessToken() ? this.oauth.getAccessToken() : null;
  }

  private refreshState(): void {
    const valid = this.oauth.hasValidAccessToken();
    this.loggedIn.set(valid);
    this.claims.set(valid ? (this.oauth.getIdentityClaims() as IdentityClaims) : null);
  }
}

/** Alleen interne paden ("/collectie?x=1"), nooit "//evil.example" of een volledige URL. */
export function safeReturnUrl(state: string | undefined | null): string | null {
  if (!state) {
    return null;
  }
  let url: string;
  try {
    url = decodeURIComponent(state);
  } catch {
    return null;
  }
  return url.startsWith('/') && !url.startsWith('//') && !url.startsWith('/\\') ? url : null;
}
