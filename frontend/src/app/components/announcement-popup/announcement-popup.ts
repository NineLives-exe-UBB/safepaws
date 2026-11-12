import { DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AnnouncementSummary, SPECIES_LABELS } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';
import { DATE_FORMAT } from '../../shared/date-format';

/** What the map shows when a pin is clicked. */
@Component({
  imports: [DatePipe, RouterLink],
  selector: 'app-announcement-popup',
  styleUrl: './announcement-popup.css',
  templateUrl: './announcement-popup.html',
})
export class AnnouncementPopup {
  private readonly announcementService = inject(AnnouncementService);

  readonly announcement = input.required<AnnouncementSummary>();

  protected readonly dateFormat = DATE_FORMAT;

  protected readonly photoUrl = computed(() =>
    this.announcementService.mainPhotoUrl(this.announcement().photos),
  );

  protected readonly title = computed(
    () => this.announcement().petName ?? SPECIES_LABELS[this.announcement().species],
  );
}
