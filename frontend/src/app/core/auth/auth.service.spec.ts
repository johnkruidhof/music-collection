import { safeReturnUrl } from './auth.service';

describe('safeReturnUrl', () => {
  it('accepteert alleen interne paden', () => {
    expect(safeReturnUrl(encodeURIComponent('/statistieken'))).toBe('/statistieken');
    expect(safeReturnUrl(encodeURIComponent('/collectie?type=CD&page=2'))).toBe(
      '/collectie?type=CD&page=2',
    );
    expect(safeReturnUrl(encodeURIComponent('//evil.example'))).toBeNull();
    expect(safeReturnUrl(encodeURIComponent('/\\evil.example'))).toBeNull();
    expect(safeReturnUrl(encodeURIComponent('https://evil.example'))).toBeNull();
    expect(safeReturnUrl('%E0%A4%A')).toBeNull();
    expect(safeReturnUrl('')).toBeNull();
    expect(safeReturnUrl(undefined)).toBeNull();
  });
});
