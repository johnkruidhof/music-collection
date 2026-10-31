import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

/**
 * OIDC-configuratie voor Keycloak (Authorization Code flow + PKCE).
 * De issuer wordt standaard afgeleid van de huidige origin, zodat dezelfde build werkt op
 * https://localhost, op een IP-adres in het thuisnetwerk en later op een publiek domein.
 */
export function authConfig(): AuthConfig {
  const origin = window.location.origin;
  return {
    issuer: environment.issuer ?? `${origin}/auth/realms/music`,
    clientId: 'music-frontend',
    responseType: 'code',
    redirectUri: `${origin}/`,
    postLogoutRedirectUri: `${origin}/`,
    scope: 'openid profile email',
    useSilentRefresh: false,
    showDebugInformation: !environment.production,
  };
}
