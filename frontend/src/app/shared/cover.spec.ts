import { TestBed } from '@angular/core/testing';
import { Cover } from './cover';

describe('Cover', () => {
  it('toont een placeholder zonder URL en na een laadfout', async () => {
    const fixture = TestBed.createComponent(Cover);
    const el = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    expect(el.querySelector('.placeholder')).not.toBeNull();

    fixture.componentRef.setInput('url', 'https://example.com/cover.jpg');
    await fixture.whenStable();
    const img = el.querySelector('img');
    expect(img?.getAttribute('src')).toBe('https://example.com/cover.jpg');

    img?.dispatchEvent(new Event('error'));
    await fixture.whenStable();
    expect(el.querySelector('img')).toBeNull();
    expect(el.querySelector('.placeholder')).not.toBeNull();
  });
});
