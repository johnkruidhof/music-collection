import { TestBed } from '@angular/core/testing';
import { ReleaseCandidate } from '../core/models/release';
import { CandidateCard } from './candidate-card';

const candidate = (ownedCount: number | null): ReleaseCandidate => ({
  source: 'DISCOGS',
  externalId: '1',
  artist: 'Radiohead',
  title: 'OK Computer',
  releaseYear: 1997,
  mediaType: 'VINYL',
  formatDetails: null,
  label: null,
  catalogNumber: null,
  country: null,
  barcode: null,
  coverThumbnailUrl: null,
  ownedCount,
});

describe('CandidateCard', () => {
  async function render(ownedCount: number | null) {
    const fixture = TestBed.createComponent(CandidateCard);
    fixture.componentRef.setInput('candidate', candidate(ownedCount));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('meldt niets voor een nieuwe release', async () => {
    const el = await render(0);
    expect(el.querySelector('.owned')).toBeNull();
    expect(el.querySelector('button')?.textContent).toContain('Toevoegen');
  });

  it('waarschuwt als de release al in de collectie staat', async () => {
    const el = await render(2);
    expect(el.querySelector('.owned')?.textContent).toContain('Al in je collectie (2×)');
    expect(el.querySelector('button')?.textContent).toContain('Nog een exemplaar');
  });
});
