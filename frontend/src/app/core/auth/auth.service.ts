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
  private readonly onlineState = signal(typeof navigator === 'undefined' ? true : navigator.onLine);

  readonly isLoggedIn = this.loggedIn.asReadonly();
  readonly online = this.onlineState.asReadonly();
  /**
   * Offline met een eerdere sessie op dit apparaat (PWA, F-26): de gecachte collectie mag bekeken worden,
   * ook als het token verlopen is. Wijzigen kan pas weer online.
   */
  readonly offlineSession = computed(
    () => !this.onlineState() && !this.loggedIn() && this.claims() !== null,
  );
  /** Ingelogd, of offline met een eerdere sessie: de app toont de beveiligde pagina's. */
  readonly hasSession = computed(() => this.loggedIn() || this.offlineSession());
  readonly displayName = computed(() => {
    const c = this.claims();
    return c?.name || c?.preferred_username || null;
  });

  /** Wordt bij het opstarten van de app aangeroepen (zie app.config.ts). */
  async init(): Promise<void> {
    window.addEventListener('online', () => {
      this.onlineState.set(true);
      this.refreshState();
    });
    window.addEventListener('offline', () => this.onlineState.set(false));
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

  /** Wist ook de offline gecachte API-gegevens, zodat een volgende gebruiker op dit apparaat die niet ziet. */
  async logout(): Promise<void> {
    await clearOfflineData();
    this.oauth.logOut();
  }

  accessToken(): string | null {
    return this.oauth.hasValidAccessToken() ? this.oauth.getAccessToken() : null;
  }

  private refreshState(): void {
    const valid = this.oauth.hasValidAccessToken();
    this.loggedIn.set(valid);
    // Offline blijven de claims van de laatste sessie bruikbaar (naam in de toolbar, offlineSession).
    const keepStored = valid || !this.onlineState();
    this.claims.set(
      keepStored ? ((this.oauth.getIdentityClaims() as IdentityClaims | null) ?? null) : null,
    );
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

/** Verwijdert de API-caches van de service worker (ngsw data groups); covers en app-bestanden blijven. */
export async function clearOfflineData(): Promise<void> {
  if (typeof caches === 'undefined') {
    return;
  }
  try {
    const names = await caches.keys();
    await Promise.all(
      names
        .filter((name) => name.includes(':data:collection-offline:'))
        .map((name) => caches.delete(name)),
    );
  } catch {
    // geen Cache Storage (bv. privévenster); niets te wissen
  }
}
