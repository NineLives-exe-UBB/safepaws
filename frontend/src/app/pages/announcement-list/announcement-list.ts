import { Component, inject, signal } from '@angular/core';
import { AnnouncementCard } from '../../components/announcement-card/announcement-card';
import { AnnouncementSummary } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';

@Component({
  imports: [AnnouncementCard],
  selector: 'app-announcement-list',
  styleUrl: './announcement-list.css',
  templateUrl: './announcement-list.html',
})
export class AnnouncementList {
  private readonly announcementService = inject(AnnouncementService);

  protected readonly announcements = signal<AnnouncementSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);

  constructor() {
    this.announcementService.getAll().subscribe({
      next: (announcements) => {
        this.announcements.set(announcements);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }
}
