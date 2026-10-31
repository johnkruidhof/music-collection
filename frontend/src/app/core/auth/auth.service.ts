import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
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
  private readonly router = inject(Router);

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

    // Na login terug naar de pagina waar de gebruiker vandaan kwam.
    const state = this.oauth.state;
    if (this.loggedIn() && state) {
      await this.router.navigateByUrl(decodeURIComponent(state));
    }
  }

  login(targetUrl = ''): void {
    this.oauth.initCodeFlow(encodeURIComponent(targetUrl));
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
