import { formatDuration, parseDuration } from './labels';

describe('formatDuration', () => {
  it('formatteert minuten en uren', () => {
    expect(formatDuration(284)).toBe('4:44');
    expect(formatDuration(59)).toBe('0:59');
    expect(formatDuration(3723)).toBe('1:02:03');
    expect(formatDuration(null)).toBe('');
  });
});

describe('parseDuration', () => {
  it('leest m:ss en h:mm:ss', () => {
    expect(parseDuration('4:44')).toBe(284);
    expect(parseDuration(' 1:02:03 ')).toBe(3723);
    expect(parseDuration('')).toBeNull();
    expect(parseDuration('4:7')).toBeUndefined();
    expect(parseDuration('abc')).toBeUndefined();
  });
});
