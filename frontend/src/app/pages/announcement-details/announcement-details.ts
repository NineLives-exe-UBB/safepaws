import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Announcement, SPECIES_LABELS } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';
import { DatePipe } from '@angular/common';
import { TYPE_LABELS } from '../../models/announcement-filters.model';
import { MapView } from '../../components/map/map';
import { MapPin } from '../../models/map.model';

type PageState = 'loading' | 'loaded' | 'not-found' | 'error';

@Component({
  imports: [DatePipe, MapView, RouterLink],
  selector: 'app-announcement-details',
  styleUrl: './announcement-details.css',
  templateUrl: './announcement-details.html',
})
export class AnnouncementDetails {
  private readonly announcementService = inject(AnnouncementService);
  private readonly title = inject(Title);

  protected readonly state = signal<PageState>('loading');
  protected readonly announcement = signal<Announcement | null>(null);

  protected readonly selectedPhotoIndex = signal(0);

  protected readonly updatingStatus = signal(false);
  protected readonly statusError = signal(false);

  /** The main photo first, so it is the one shown when the page opens. */
  protected readonly photoUrls = computed(() => {
    const photos = this.announcement()?.photos ?? [];
    return [...photos]
      .sort((a, b) => Number(b.isMain) - Number(a.isMain))
      .map((photo) => this.announcementService.photoUrl(photo));
  });

  protected readonly selectedPhotoUrl = computed(
    () => this.photoUrls()[this.selectedPhotoIndex()] ?? null,
  );

  /** The pet's name, or its species when it has no name (e.g. a found pet). */
  protected readonly heading = computed(() => {
    const announcement = this.announcement();
    return announcement ? (announcement.petName ?? SPECIES_LABELS[announcement.species]) : '';
  });

  /** E.g. "Lost cat" or "Found dog". */
  protected readonly subtitle = computed(() => {
    const announcement = this.announcement();
    return announcement
      ? `${TYPE_LABELS[announcement.type]} ${SPECIES_LABELS[announcement.species].toLowerCase()}`
      : '';
  });

  protected readonly posterUrl = computed(() => {
    const announcement = this.announcement();
    return announcement ? this.announcementService.posterUrl(announcement.id) : null;
  });

  /** One pin for the announcement, or none when it has no coordinates. */
  protected readonly pins = computed<MapPin[]>(() => {
    const announcement = this.announcement();
    if (announcement?.latitude == null || announcement.longitude == null) {
      return [];
    }
    return [
      {
        latitude: announcement.latitude,
        longitude: announcement.longitude,
        type: announcement.type,
        label: announcement.area,
      },
    ];
  });

  constructor() {
    const id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

    if (!Number.isInteger(id) || id <= 0) {
      this.state.set('not-found');
      return;
    }

    this.announcementService.getById(id).subscribe({
      next: (announcement) => {
        this.announcement.set(announcement);
        this.state.set('loaded');
        this.title.setTitle(`${this.heading()} | SafePaws`);
      },
      error: (error: unknown) => {
        const notFound = error instanceof HttpErrorResponse && error.status === 404;
        this.state.set(notFound ? 'not-found' : 'error');
      },
    });
  }

  protected selectPhoto(index: number): void {
    this.selectedPhotoIndex.set(index);
  }

  protected toggleStatus(): void {
    const announcement = this.announcement();
    if (!announcement || this.updatingStatus()) {
      return;
    }

    const status = announcement.status === 'ACTIVE' ? 'RESOLVED' : 'ACTIVE';
    this.updatingStatus.set(true);
    this.statusError.set(false);

    this.announcementService.updateStatus(announcement.id, status).subscribe({
      next: (updated) => {
        this.announcement.set(updated);
        this.updatingStatus.set(false);
      },
      error: () => {
        this.statusError.set(true);
        this.updatingStatus.set(false);
      },
    });
  }
}
