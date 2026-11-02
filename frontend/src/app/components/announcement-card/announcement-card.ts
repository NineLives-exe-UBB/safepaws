import { DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { Announcement } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';

@Component({
  imports: [DatePipe],
  selector: 'app-announcement-card',
  styleUrl: './announcement-card.css',
  templateUrl: './announcement-card.html',
})
export class AnnouncementCard {
  private readonly announcementService = inject(AnnouncementService);

  readonly announcement = input.required<Announcement>();

  protected readonly mainPhotoUrl = computed(() => {
    const photos = this.announcement().photos;
    const mainPhoto = photos.find((photo) => photo.isMain) ?? photos[0];
    return mainPhoto ? this.announcementService.photoUrl(mainPhoto) : null;
  });

  protected readonly posterUrl = computed(() =>
    this.announcementService.posterUrl(this.announcement().id),
  );
}
