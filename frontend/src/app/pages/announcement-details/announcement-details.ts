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
}