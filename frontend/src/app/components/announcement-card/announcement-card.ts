import { DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { AnnouncementSummary, SPECIES_LABELS } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';

@Component({
  imports: [DatePipe],
  selector: 'app-announcement-card',
  styleUrl: './announcement-card.css',
  templateUrl: './announcement-card.html',
})
export class AnnouncementCard {
  private readonly announcementService = inject(AnnouncementService);

  readonly announcement = input.required<AnnouncementSummary>();

  protected readonly mainPhotoUrl = computed(() => {
    const photos = this.announcement().photos;
    const mainPhoto = photos.find((photo) => photo.isMain) ?? photos[0];
    return mainPhoto ? this.announcementService.photoUrl(mainPhoto) : null;
  });

  protected readonly speciesLabel = computed(() => SPECIES_LABELS[this.announcement().species]);

  protected readonly distanceText = computed(() => {
    const distanceKm = this.announcement().distanceKm;
    return distanceKm == null ? null : `${distanceKm.toFixed(1)} km away`;
  });

  protected readonly posterUrl = computed(() =>
    this.announcementService.posterUrl(this.announcement().id),
  );
}
