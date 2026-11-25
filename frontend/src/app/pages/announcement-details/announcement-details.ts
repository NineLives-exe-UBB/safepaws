
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, formatDate } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Announcement, SPECIES_LABELS } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';
import { TYPE_LABELS } from '../../models/announcement-filters.model';
import { MapView } from '../../components/map/map';
import { Coordinates, MapPin } from '../../models/map.model';
import { MAX_NOTE_LENGTH, Sighting } from '../../models/sighting.model';
import { DATE_FORMAT, DAY_FORMAT, TIME_FORMAT, toDateTimeInputValue } from '../../shared/date-format';
import { AuthService } from '../../services/auth.service';
import { SightingService } from '../../services/sighting.service';

type PageState = 'loading' | 'loaded' | 'not-found' | 'error';

@Component({
  imports: [DatePipe, MapView, ReactiveFormsModule, RouterLink],
  selector: 'app-announcement-details',
  styleUrl: './announcement-details.css',
  templateUrl: './announcement-details.html',
})
export class AnnouncementDetails {
  private readonly announcementService = inject(AnnouncementService);
  private readonly sightingService = inject(SightingService);
  private readonly title = inject(Title);
  private readonly fb = inject(FormBuilder);

  protected readonly authService = inject(AuthService);
  protected readonly dateFormat = DATE_FORMAT;
  protected readonly dayFormat = DAY_FORMAT;
  protected readonly timeFormat = TIME_FORMAT;

  protected readonly state = signal<PageState>('loading');
  protected readonly announcement = signal<Announcement | null>(null);

  protected readonly isOwner = computed(() => {
    const announcement = this.announcement();
    const user = this.authService.user();

    return announcement !== null &&
      user !== null &&
      announcement.ownerId === user.id;
  });

  protected readonly selectedPhotoIndex = signal(0);

  protected readonly updatingStatus = signal(false);
  protected readonly statusError = signal(false);

  protected readonly photoUrls = computed(() => {
    const photos = this.announcement()?.photos ?? [];

    return [...photos]
      .sort((a, b) => Number(b.isMain) - Number(a.isMain))
      .map((photo) => this.announcementService.photoUrl(photo));
  });

  protected readonly selectedPhotoUrl = computed(
    () => this.photoUrls()[this.selectedPhotoIndex()] ?? null,
  );

  protected readonly heading = computed(() => {
    const announcement = this.announcement();

    return announcement
      ? (announcement.petName ?? SPECIES_LABELS[announcement.species])
      : '';
  });

  protected readonly subtitle = computed(() => {
    const announcement = this.announcement();

    return announcement
      ? `${TYPE_LABELS[announcement.type]} ${SPECIES_LABELS[announcement.species].toLowerCase()}`
      : '';
  });

  protected readonly typeLabel = computed(() => {
    const announcement = this.announcement();

    return announcement ? TYPE_LABELS[announcement.type] : '';
  });

  protected readonly posterUrl = computed(() => {
    const announcement = this.announcement();

    return announcement
      ? this.announcementService.posterUrl(announcement.id)
      : null;
  });

  /** The trail is for lost pets: a found pet is already with someone. */
  protected readonly showTrail = computed(() => this.announcement()?.type === 'LOST');

  /** The backend only accepts sightings for a lost pet that is still being looked for. */
  protected readonly canAddSighting = computed(() => {
    const announcement = this.announcement();

    return announcement?.type === 'LOST' && announcement.status === 'ACTIVE';
  });

  protected readonly sightings = signal<Sighting[]>([]);
  protected readonly sightingsFailed = signal(false);
  protected readonly sightingError = signal('');
  protected readonly sightingSaved = signal(false);

  /** The place the user clicked, until the sighting is saved or cancelled. */
  protected readonly draft = signal<Coordinates | null>(null);
  protected readonly savingSighting = signal(false);
  protected readonly sightingFormError = signal('');

  protected readonly maxNoteLength = MAX_NOTE_LENGTH;

  protected readonly sightingForm = this.fb.nonNullable.group({
    seenAt: [
      '',
      [
        Validators.required,
        (control: AbstractControl): ValidationErrors | null => this.seenAtError(control.value),
      ],
    ],
    note: ['', Validators.maxLength(MAX_NOTE_LENGTH)],
  });

  /** The limits of the date picker: from the day the pet was lost until now. */
  protected readonly seenAtMin = computed(
    () => this.announcement()?.eventDate.slice(0, 16) ?? '',
  );
  protected seenAtMax = '';

  /** The sightings in the order the pet was seen. */
  private readonly trailSightings = computed(() =>
    [...this.sightings()].sort(
      (a, b) => new Date(a.seenAt).getTime() - new Date(b.seenAt).getTime() || a.id - b.id,
    ),
  );

  /** Where the pet was lost, if the announcement has a location. */
  private readonly lostPoint = computed<Coordinates | null>(() => {
    const announcement = this.announcement();

    return announcement?.latitude == null || announcement.longitude == null
      ? null
      : { latitude: announcement.latitude, longitude: announcement.longitude };
  });

  protected readonly pins = computed<MapPin[]>(() => {
    const announcement = this.announcement();
    const lostPoint = this.lostPoint();
    const pins: MapPin[] = [];

    if (announcement && lostPoint) {
      pins.push({ ...lostPoint, type: announcement.type, label: announcement.area });
    }

    if (this.showTrail()) {
      this.trailSightings().forEach((sighting) =>
        pins.push({
          latitude: sighting.latitude,
          longitude: sighting.longitude,
          type: 'SIGHTING',
          id: sighting.id,
          label: `Seen on ${formatDate(sighting.seenAt, DATE_FORMAT, 'en-US')}`,
        }),
      );
    }

    const draft = this.draft();

    if (draft) {
      pins.push({ ...draft, type: 'DRAFT', label: 'New sighting' });
    }

    return pins;
  });

  /** Where the pet was lost, then every sighting in the order it was seen. */
  protected readonly trail = computed<Coordinates[]>(() => {
    const lostPoint = this.lostPoint();
    const sightings = this.trailSightings().map(({ latitude, longitude }) => ({
      latitude,
      longitude,
    }));

    return lostPoint ? [lostPoint, ...sightings] : sightings;
  });

  /** A single location is shown close up; with a trail the map zooms to show all the points. */
  protected readonly mapCenter = computed(() =>
    this.showTrail() && this.sightings().length > 0 ? null : (this.pins()[0] ?? null),
  );

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
        this.loadSightings(announcement);
      },
      error: (error: unknown) => {
        const notFound =
          error instanceof HttpErrorResponse && error.status === 404;

        this.state.set(notFound ? 'not-found' : 'error');
      },
    });
  }

  protected selectPhoto(index: number): void {
    this.selectedPhotoIndex.set(index);
  }

  protected toggleStatus(): void {
    const announcement = this.announcement();

    if (!announcement || !this.isOwner() || this.updatingStatus()) {
      return;
    }

    const status =
      announcement.status === 'ACTIVE' ? 'RESOLVED' : 'ACTIVE';

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

  protected sightingFor(pin: MapPin): Sighting | undefined {
    return pin.type === 'SIGHTING'
      ? this.sightings().find((sighting) => sighting.id === pin.id)
      : undefined;
  }

  /** The owner of the announcement and the author of the sighting may delete it. */
  protected canDelete(sighting: Sighting): boolean {
    const user = this.authService.user();

    return user !== null && (user.id === sighting.reporter.id || this.isOwner());
  }

  /** A click on the map starts a new sighting, for a logged-in user. */
  protected startSighting(location: Coordinates): void {
    if (!this.canAddSighting() || this.authService.user() === null || this.savingSighting()) {
      return;
    }

    const now = new Date();

    this.seenAtMax = toDateTimeInputValue(now);
    this.sightingForm.reset({ seenAt: this.seenAtMax, note: '' });
    this.sightingFormError.set('');
    this.sightingSaved.set(false);
    this.draft.set(location);
  }

  protected cancelSighting(): void {
    this.draft.set(null);
    this.sightingFormError.set('');
  }

  protected saveSighting(): void {
    const announcement = this.announcement();
    const draft = this.draft();

    this.sightingForm.markAllAsTouched();

    if (!announcement || !draft || this.sightingForm.invalid || this.savingSighting()) {
      return;
    }

    const { seenAt, note } = this.sightingForm.getRawValue();
    const trimmedNote = note.trim();

    this.savingSighting.set(true);
    this.sightingFormError.set('');

    this.sightingService
      .create(announcement.id, {
        ...draft,
        seenAt: seenAt.length === 16 ? `${seenAt}:00` : seenAt,
        ...(trimmedNote ? { note: trimmedNote } : {}),
      })
      .subscribe({
        next: (sighting) => {
          this.sightings.update((sightings) => [...sightings, sighting]);
          this.draft.set(null);
          this.savingSighting.set(false);
          this.sightingSaved.set(true);
        },
        error: (error: unknown) => {
          this.sightingFormError.set(this.saveErrorMessage(error));
          this.savingSighting.set(false);
        },
      });
  }

  protected deleteSighting(sighting: Sighting): void {
    if (!this.canDelete(sighting)) {
      return;
    }

    this.sightingError.set('');
    this.sightingSaved.set(false);

    this.sightingService.delete(sighting.id).subscribe({
      next: () => this.removeSighting(sighting.id),
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          // somebody deleted it first, so it is gone either way
          this.removeSighting(sighting.id);
          return;
        }

        this.sightingError.set(
          error instanceof HttpErrorResponse && error.status === 403
            ? 'You can only delete your own sightings or sightings on your announcements.'
            : 'Could not delete the sighting. Please try again.',
        );
      },
    });
  }

  private removeSighting(id: number): void {
    this.sightings.update((sightings) => sightings.filter((sighting) => sighting.id !== id));
  }

  private loadSightings(announcement: Announcement): void {
    if (announcement.type !== 'LOST') {
      return;
    }

    this.sightingService.getByAnnouncement(announcement.id).subscribe({
      next: (sightings) => this.sightings.set(sightings),
      error: () => this.sightingsFailed.set(true),
    });
  }

  /** The pet cannot be seen in the future, or before it was lost. */
  private seenAtError(value: string): ValidationErrors | null {
    const seenAt = new Date(value);
    const announcement = this.announcement();

    if (!value || Number.isNaN(seenAt.getTime())) {
      return null;
    }

    if (seenAt.getTime() > Date.now()) {
      return { future: true };
    }

    if (announcement && seenAt.getTime() < new Date(announcement.eventDate).getTime()) {
      return { beforeLost: true };
    }

    return null;
  }

  /** The messages of the backend have no full stop, the ones on the page have. */
  private saveErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 400) {
      const fieldErrors: { message: string }[] = error.error?.fieldErrors ?? [];
      const messages = fieldErrors.map(({ message }) => message);

      if (messages.length === 0 && typeof error.error?.message === 'string') {
        messages.push(error.error.message);
      }

      if (messages.length > 0) {
        return messages.map((message) => withFullStop(message)).join(' ');
      }
    }

    return 'Could not save the sighting. Please try again.';
  }
}

function withFullStop(message: string): string {
  const trimmed = message.trim();

  return trimmed.endsWith('.') ? trimmed : `${trimmed}.`;
}
