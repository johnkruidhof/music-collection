export const environment = {
  production: true,
  /** null = afleiden van de huidige origin: https://<host>/auth/realms/music (via Caddy). */
  issuer: null as string | null,
};
