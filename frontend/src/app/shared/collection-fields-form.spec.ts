import { collectionFieldsValue, createCollectionFieldsForm } from './collection-fields-form';

describe('collection fields form', () => {
  it('zet lege velden om naar null en leest een prijs met komma', () => {
    const form = createCollectionFieldsForm();
    form.patchValue({ purchasePrice: '24,99', storageLocation: '  Kast A ', notes: '   ' });

    expect(collectionFieldsValue(form)).toEqual({
      conditionMedia: null,
      conditionSleeve: null,
      purchaseDate: null,
      purchasePrice: 24.99,
      purchaseLocation: null,
      storageLocation: 'Kast A',
      notes: null,
    });
  });

  it('vult het formulier vanuit bestaande gegevens en valideert de prijs', () => {
    const form = createCollectionFieldsForm({
      conditionMedia: 'NEAR_MINT',
      conditionSleeve: null,
      purchaseDate: '2024-05-01',
      purchasePrice: 12.5,
      purchaseLocation: null,
      storageLocation: null,
      notes: null,
    });

    expect(form.controls.purchasePrice.value).toBe('12,5');
    form.controls.purchasePrice.setValue('twaalf');
    expect(form.controls.purchasePrice.invalid).toBe(true);
  });
});
