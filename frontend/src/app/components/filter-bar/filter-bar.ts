import { Component, effect, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, merge } from 'rxjs';
import { SPECIES_LABELS } from '../../models/announcement.model';
import {
  AnnouncementFilters,
  DEFAULT_FILTERS,
  LocationStatus,
  RADIUS_OPTIONS_KM,
  SORT_LABELS,
  STATUS_LABELS,
  TYPE_LABELS,
  hasActiveFilters,
} from '../../models/announcement-filters.model';

const TEXT_DEBOUNCE_MS = 300;

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-filter-bar',
  styleUrl: './filter-bar.css',
  templateUrl: './filter-bar.html',
})
export class FilterBar {
  /** The filters to show, e.g. the ones read from the URL. */
  readonly filters = input<AnnouncementFilters>(DEFAULT_FILTERS);
  /** Radius and distance sorting are only available once the browser location is known. */
  readonly locationStatus = input<LocationStatus>('pending');

  readonly filtersChange = output<AnnouncementFilters>();

  protected readonly typeOptions = Object.entries(TYPE_LABELS);
  protected readonly speciesOptions = Object.entries(SPECIES_LABELS);
  protected readonly statusOptions = Object.entries(STATUS_LABELS);
  protected readonly sortOptions = Object.entries(SORT_LABELS);
  protected readonly radiusOptions = RADIUS_OPTIONS_KM;

  protected readonly form = inject(NonNullableFormBuilder).group({
    type: DEFAULT_FILTERS.type,
    species: DEFAULT_FILTERS.species,
    status: DEFAULT_FILTERS.status,
    color: DEFAULT_FILTERS.color,
    breed: DEFAULT_FILTERS.breed,
    area: DEFAULT_FILTERS.area,
    radiusKm: DEFAULT_FILTERS.radiusKm as number | null,
    sort: DEFAULT_FILTERS.sort,
  });

  private lastEmitted = DEFAULT_FILTERS;

  constructor() {
    const { type, species, status, radiusKm, sort, color, breed, area } = this.form.controls;
    const choices = merge(type.valueChanges, species.valueChanges, status.valueChanges);
    const location = merge(radiusKm.valueChanges, sort.valueChanges);
    const typing = merge(color.valueChanges, breed.valueChanges, area.valueChanges).pipe(
      debounceTime(TEXT_DEBOUNCE_MS),
    );
    merge(choices, location, typing)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.emit());

    effect(() => {
      const filters = this.filters();
      if (JSON.stringify(filters) !== JSON.stringify(this.lastEmitted)) {
        this.lastEmitted = filters;
        this.form.setValue(filters, { emitEvent: false });
      }
    });

    effect(() => {
      if (this.locationAvailable()) {
        radiusKm.enable({ emitEvent: false });
      } else {
        radiusKm.disable({ emitEvent: false });
      }
    });
  }

  protected locationAvailable(): boolean {
    return this.locationStatus() === 'granted';
  }

  protected hasActiveFilters(): boolean {
    return hasActiveFilters(this.form.getRawValue());
  }

  protected clearFilters(): void {
    this.form.reset(
      { ...DEFAULT_FILTERS, sort: this.form.getRawValue().sort },
      { emitEvent: false },
    );
    this.emit();
  }

  private emit(): void {
    const values = this.form.getRawValue();
    const filters: AnnouncementFilters = {
      ...values,
      color: values.color.trim(),
      breed: values.breed.trim(),
      area: values.area.trim(),
    };
    this.lastEmitted = filters;
    this.filtersChange.emit(filters);
  }
}
