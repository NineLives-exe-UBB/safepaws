import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Announcement, SPECIES_LABELS } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';

type PageState = 'loading' | 'loaded' | 'not-found' | 'error';

@Component({
  imports: [RouterLink],
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
}