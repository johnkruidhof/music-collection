export const environment = {
  production: false,
  /** Bij `ng serve` (http://localhost:4200) draait Keycloak achter Caddy op https://localhost/auth. */
  issuer: 'https://localhost/auth/realms/music' as string | null,
};
