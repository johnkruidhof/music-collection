import { discFromPosition, parseTrackLines } from './track-parser';

describe('discFromPosition', () => {
  it('leidt het schijfnummer af zoals de backend', () => {
    expect(discFromPosition('A1')).toBe(1);
    expect(discFromPosition('B3')).toBe(1);
    expect(discFromPosition('C1')).toBe(2);
    expect(discFromPosition('D2a')).toBe(2);
    expect(discFromPosition('2-5')).toBe(2);
    expect(discFromPosition('CD3-1')).toBe(3);
    expect(discFromPosition('7')).toBe(1);
    expect(discFromPosition('')).toBe(1);
  });
});

describe('parseTrackLines', () => {
  it('herkent positie, titel en duur in gangbare notaties', () => {
    const tracks = parseTrackLines(`
      A1 Airbag 4:44
      A2  Paranoid Android (6:23)
      1. Subterranean Homesick Alien
      2-1 Karma Police – 4:21
      Zonder positie
    `);

    expect(tracks).toEqual([
      { discNumber: 1, position: 'A1', title: 'Airbag', duration: '4:44' },
      { discNumber: 1, position: 'A2', title: 'Paranoid Android', duration: '6:23' },
      { discNumber: 1, position: '1', title: 'Subterranean Homesick Alien', duration: null },
      { discNumber: 2, position: '2-1', title: 'Karma Police', duration: '4:21' },
      { discNumber: 1, position: null, title: 'Zonder positie', duration: null },
    ]);
  });
});
