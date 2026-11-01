import { Component, input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CollectionItemFields } from '../core/models/collection';
import { MEDIA_CONDITIONS, MediaCondition } from '../core/models/release';
import { ConditionPipe } from './label.pipes';

export type CollectionFieldsForm = FormGroup<{
  conditionMedia: FormControl<MediaCondition | null>;
  conditionSleeve: FormControl<MediaCondition | null>;
  purchaseDate: FormControl<string>;
  purchasePrice: FormControl<string>;
  purchaseLocation: FormControl<string>;
  storageLocation: FormControl<string>;
  notes: FormControl<string>;
}>;

export function createCollectionFieldsForm(
  fields?: CollectionItemFields | null,
): CollectionFieldsForm {
  return new FormGroup({
    conditionMedia: new FormControl<MediaCondition | null>(fields?.conditionMedia ?? null),
    conditionSleeve: new FormControl<MediaCondition | null>(fields?.conditionSleeve ?? null),
    purchaseDate: new FormControl(fields?.purchaseDate ?? '', { nonNullable: true }),
    purchasePrice: new FormControl(
      fields?.purchasePrice !== null && fields?.purchasePrice !== undefined
        ? String(fields.purchasePrice).replace('.', ',')
        : '',
      { nonNullable: true, validators: [Validators.pattern(/^\s*\d{1,8}([.,]\d{1,2})?\s*$/)] },
    ),
    purchaseLocation: new FormControl(fields?.purchaseLocation ?? '', {
      nonNullable: true,
      validators: [Validators.maxLength(255)],
    }),
    storageLocation: new FormControl(fields?.storageLocation ?? '', {
      nonNullable: true,
      validators: [Validators.maxLength(255)],
    }),
    notes: new FormControl(fields?.notes ?? '', {
      nonNullable: true,
      validators: [Validators.maxLength(4000)],
    }),
  });
}

/** Formulierwaarde → DTO; lege velden worden {@code null}, prijs accepteert komma of punt. */
export function collectionFieldsValue(form: CollectionFieldsForm): CollectionItemFields {
  const v = form.getRawValue();
  const text = (value: string) => (value.trim() === '' ? null : value.trim());
  const price = text(v.purchasePrice);
  return {
    conditionMedia: v.conditionMedia,
    conditionSleeve: v.conditionSleeve,
    purchaseDate: text(v.purchaseDate),
    purchasePrice: price === null ? null : Number(price.replace(',', '.')),
    purchaseLocation: text(v.purchaseLocation),
    storageLocation: text(v.storageLocation),
    notes: text(v.notes),
  };
}

/** De persoonlijke velden (staat, aankoop, locatie, notities); bij toevoegen en op de detailpagina. */
@Component({
  selector: 'app-collection-fields-form',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ConditionPipe,
  ],
  templateUrl: './collection-fields-form.html',
  styleUrl: './collection-fields-form.scss',
})
export class CollectionFieldsFormComponent {
  readonly form = input.required<CollectionFieldsForm>();
  protected readonly conditions = MEDIA_CONDITIONS;
  protected readonly today = new Date().toISOString().slice(0, 10);
}
