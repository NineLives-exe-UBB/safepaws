import { computed, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, distinctUntilChanged, filter, map, of, switchMap, tap } from 'rxjs';
import { AnnouncementSummary } from '../models/announcement.model';
import {
  AnnouncementFilters,
  DEFAULT_FILTERS,
  LocationStatus,
  UserLocation,
  fromQueryParams,
  hasActiveFilters,
  needsLocation,
  toQueryParams,
  withoutLocationFilters,
} from '../models/announcement-filters.model';
import { AnnouncementService } from './announcement.service';
import { LocationService } from './location.service';

/**
 * The filters in the URL, the user's location and the announcements they give. Every page that
 * shows the announcements provides its own instance, so they all show the same results for the
 * same filters.
 */
@Injectable()
export class AnnouncementSearch {
  private readonly announcementService = inject(AnnouncementService);
  private readonly locationService = inject(LocationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly urlFilters = toSignal(
    this.route.queryParamMap.pipe(
      map((params) => fromQueryParams((name) => params.get(name))),
      distinctUntilChanged((first, second) => JSON.stringify(first) === JSON.stringify(second)),
    ),
    { initialValue: DEFAULT_FILTERS },
  );
  private readonly location = signal<UserLocation | null>(null);

  readonly userLocation = this.location.asReadonly();
  readonly locationStatus = signal<LocationStatus>('pending');
  readonly announcements = signal<AnnouncementSummary[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);

  /** Without the browser location, radius and distance sorting cannot be used. */
  readonly filters = computed(() =>
    this.locationStatus() === 'denied'
      ? withoutLocationFilters(this.urlFilters())
      : this.urlFilters(),
  );
  readonly filtersActive = computed(() => hasActiveFilters(this.filters()));

  /** Null while waiting for a location that the filters need. */
  private readonly request = computed(() => {
    const filters = this.filters();
    if (!needsLocation(filters)) {
      return { filters, location: null };
    }
    const location = this.location();
    return location ? { filters, location } : null;
  });

  constructor() {
    this.locationService.getCurrentLocation().then((location) => {
      this.location.set(location);
      this.locationStatus.set(location ? 'granted' : 'denied');
    });

    toObservable(this.request)
      .pipe(
        filter((request): request is NonNullable<typeof request> => request !== null),
        distinctUntilChanged((first, second) => JSON.stringify(first) === JSON.stringify(second)),
        tap(() => this.loading.set(true)),
        switchMap(({ filters, location }) =>
          this.announcementService.getAll(filters, location).pipe(
            map((announcements) => ({ announcements, failed: false })),
            catchError(() => of({ announcements: [], failed: true })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(({ announcements, failed }) => {
        this.announcements.set(announcements);
        this.failed.set(failed);
        this.loading.set(false);
      });
  }

  updateFilters(filters: AnnouncementFilters): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: toQueryParams(filters),
      replaceUrl: true,
    });
  }
}
