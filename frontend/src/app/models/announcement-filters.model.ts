import {
  AnnouncementStatus,
  AnnouncementType,
  SPECIES_LABELS,
  Species,
} from './announcement.model';

export type SortOption = 'newest' | 'oldest' | 'closest';
export type LocationStatus = 'pending' | 'granted' | 'denied';

export interface UserLocation {
  latitude: number;
  longitude: number;
}

export interface AnnouncementFilters {
  type: AnnouncementType | '';
  species: Species | '';
  status: AnnouncementStatus;
  color: string;
  breed: string;
  area: string;
  radiusKm: number | null;
  sort: SortOption;
}

export const DEFAULT_FILTERS: AnnouncementFilters = {
  type: '',
  species: '',
  status: 'ACTIVE',
  color: '',
  breed: '',
  area: '',
  radiusKm: null,
  sort: 'newest',
};

export const TYPE_LABELS: Record<AnnouncementType, string> = {
  LOST: 'Lost',
  FOUND: 'Found',
};

export const STATUS_LABELS: Record<AnnouncementStatus, string> = {
  ACTIVE: 'Active',
  RESOLVED: 'Resolved',
};

export const SORT_LABELS: Record<SortOption, string> = {
  newest: 'Newest first',
  oldest: 'Oldest first',
  closest: 'Closest first',
};

export const RADIUS_OPTIONS_KM = [1, 5, 10, 25, 50, 100];
/** The backend limits the radius to 1-100 km and uses 10 km when none is sent with a location. */
export const MAX_RADIUS_KM = 100;

export const TEXT_FILTER_MAX_LENGTH = { color: 100, breed: 100, area: 150 };

const TEXT_FILTERS = ['color', 'breed', 'area'] as const;

export function hasActiveFilters(filters: AnnouncementFilters): boolean {
  return (
    filters.type !== DEFAULT_FILTERS.type ||
    filters.species !== DEFAULT_FILTERS.species ||
    filters.status !== DEFAULT_FILTERS.status ||
    filters.radiusKm !== DEFAULT_FILTERS.radiusKm ||
    TEXT_FILTERS.some((name) => filters[name] !== DEFAULT_FILTERS[name])
  );
}

export function needsLocation(filters: AnnouncementFilters): boolean {
  return filters.radiusKm !== null || filters.sort === 'closest';
}

/** Only the values that differ from the defaults, so the URL stays short. */
export function toQueryParams(filters: AnnouncementFilters): Record<string, string> {
  const params: Record<string, string> = {};
  const textAndSelects = ['type', 'species', 'status', ...TEXT_FILTERS] as const;
  textAndSelects.forEach((name) => {
    if (filters[name] !== DEFAULT_FILTERS[name]) {
      params[name] = filters[name];
    }
  });
  if (filters.radiusKm !== null) {
    params['radiusKm'] = String(filters.radiusKm);
  }
  if (filters.sort !== DEFAULT_FILTERS.sort) {
    params['sort'] = filters.sort;
  }
  return params;
}

/** Reads the filters from the URL; anything unknown or invalid falls back to the default. */
export function fromQueryParams(get: (name: string) => string | null): AnnouncementFilters {
  const oneOf = <T extends string>(
    name: string,
    allowed: Record<T, string>,
    fallback: T | '',
  ): T | '' => {
    const value = get(name);
    return value !== null && value in allowed ? (value as T) : fallback;
  };
  const text = (name: (typeof TEXT_FILTERS)[number]): string => get(name)?.trim() ?? '';
  const radius = Number(get('radiusKm'));

  return {
    type: oneOf('type', TYPE_LABELS, DEFAULT_FILTERS.type),
    species: oneOf('species', SPECIES_LABELS, DEFAULT_FILTERS.species),
    status: (oneOf('status', STATUS_LABELS, DEFAULT_FILTERS.status) ||
      DEFAULT_FILTERS.status) as AnnouncementStatus,
    color: text('color'),
    breed: text('breed'),
    area: text('area'),
    radiusKm: RADIUS_OPTIONS_KM.includes(radius) ? radius : DEFAULT_FILTERS.radiusKm,
    sort: oneOf('sort', SORT_LABELS, DEFAULT_FILTERS.sort) as SortOption,
  };
}

/** What can be asked from the backend when the browser location is not available. */
export function withoutLocationFilters(filters: AnnouncementFilters): AnnouncementFilters {
  return {
    ...filters,
    radiusKm: null,
    sort: filters.sort === 'closest' ? DEFAULT_FILTERS.sort : filters.sort,
  };
}
