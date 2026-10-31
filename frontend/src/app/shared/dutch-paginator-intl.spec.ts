import { DutchPaginatorIntl } from './dutch-paginator-intl';

describe('DutchPaginatorIntl', () => {
  it('toont het bereik in het Nederlands', () => {
    const intl = new DutchPaginatorIntl();
    expect(intl.getRangeLabel(0, 24, 100)).toBe('1 – 24 van 100');
    expect(intl.getRangeLabel(4, 24, 100)).toBe('97 – 100 van 100');
    expect(intl.getRangeLabel(0, 24, 0)).toBe('0 van 0');
  });
});
