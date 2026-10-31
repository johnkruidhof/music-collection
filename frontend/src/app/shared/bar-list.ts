import { Component, computed, input } from '@angular/core';

export interface BarRow {
  label: string;
  value: number;
}

/**
 * Horizontaal staafdiagram met één serie: label links, staaf vanaf één basislijn, waarde aan het eind.
 * Eén kleur voor alle staven (de lengte draagt de grootte), dunne staven met afgeronde kop, tekst in
 * tekstkleur. Elke rij heeft een tooltip en is als lijst toegankelijk (de waarden staan als tekst).
 */
@Component({
  selector: 'app-bar-list',
  template: `
    <ul class="bars" [attr.aria-label]="label()">
      @for (row of rows(); track row.label) {
        <li class="row" [attr.title]="row.label + ': ' + format()(row.value)">
          <span class="label">{{ row.label }}</span>
          <span class="track">
            <span class="bar" [style.width.%]="percentage(row.value)"></span>
            <span class="value">{{ format()(row.value) }}</span>
          </span>
        </li>
      }
    </ul>
  `,
  styles: `
    .bars {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 2px;
    }
    .row {
      display: grid;
      grid-template-columns: minmax(6em, 11em) 1fr;
      align-items: center;
      gap: 12px;
      min-height: 28px;
      padding: 0 4px;
      border-radius: 4px;
    }
    .row:hover {
      background: var(--mat-sys-surface-container);
    }
    .label {
      color: var(--mat-sys-on-surface-variant);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .track {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;
    }
    .bar {
      height: 12px;
      min-width: 2px;
      border-radius: 0 4px 4px 0;
      background: var(--mat-sys-primary);
    }
    .row:hover .bar {
      background: var(--mat-sys-on-primary-container);
    }
    .value {
      color: var(--mat-sys-on-surface);
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
    }
  `,
})
export class BarList {
  readonly rows = input.required<BarRow[]>();
  /** Toegankelijke naam van de lijst (de zichtbare titel staat erboven). */
  readonly label = input('');
  readonly format = input<(value: number) => string>((value) => String(value));

  private readonly max = computed(() => Math.max(0, ...this.rows().map((r) => r.value)));

  /** Ruimte voor de waarde rechts: de langste staaf vult maximaal 85% van de rij. */
  protected percentage(value: number): number {
    const max = this.max();
    return max === 0 ? 0 : (value / max) * 85;
  }
}
