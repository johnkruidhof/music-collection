import { Pipe, PipeTransform } from '@angular/core';
import { MediaCondition, MediaType, MetadataSource } from '../core/models/release';
import { CONDITION_LABELS, MEDIA_TYPE_LABELS, SOURCE_LABELS, formatDuration } from './labels';

@Pipe({ name: 'mediaType' })
export class MediaTypePipe implements PipeTransform {
  transform(value: MediaType | null | undefined): string {
    return value ? MEDIA_TYPE_LABELS[value] : '';
  }
}

@Pipe({ name: 'condition' })
export class ConditionPipe implements PipeTransform {
  transform(value: MediaCondition | null | undefined): string {
    return value ? CONDITION_LABELS[value] : '';
  }
}

@Pipe({ name: 'source' })
export class SourcePipe implements PipeTransform {
  transform(value: MetadataSource | null | undefined): string {
    return value ? SOURCE_LABELS[value] : '';
  }
}

@Pipe({ name: 'duration' })
export class DurationPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return formatDuration(value);
  }
}
