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
  templateUrl: './bar-list.html',
  styleUrl: './bar-list.scss',
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
