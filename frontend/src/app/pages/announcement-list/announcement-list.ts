import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, distinctUntilChanged, filter, map, of, switchMap, tap } from 'rxjs';
import { AnnouncementCard } from '../../components/announcement-card/announcement-card';
import { FilterBar } from '../../components/filter-bar/filter-bar';
import { AnnouncementSummary } from '../../models/announcement.model';
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
} from '../../models/announcement-filters.model';
import { AnnouncementService } from '../../services/announcement.service';
import { LocationService } from '../../services/location.service';

@Component({
  imports: [AnnouncementCard, FilterBar],
  selector: 'app-announcement-list',
  styleUrl: './announcement-list.css',
  templateUrl: './announcement-list.html',
})
export class AnnouncementList {
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

  protected readonly locationStatus = signal<LocationStatus>('pending');
  protected readonly announcements = signal<AnnouncementSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);

  /** Without the browser location, radius and distance sorting cannot be used. */
  protected readonly filters = computed(() =>
    this.locationStatus() === 'denied'
      ? withoutLocationFilters(this.urlFilters())
      : this.urlFilters(),
  );
  protected readonly filtersActive = computed(() => hasActiveFilters(this.filters()));

  /** Null while the page waits for a location that the filters need. */
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

  protected updateFilters(filters: AnnouncementFilters): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: toQueryParams(filters),
      replaceUrl: true,
    });
  }
}
