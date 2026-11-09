import { Component, inject } from '@angular/core';
import { AnnouncementCard } from '../../components/announcement-card/announcement-card';
import { FilterBar } from '../../components/filter-bar/filter-bar';
import { AnnouncementSearch } from '../../services/announcement-search';

@Component({
  imports: [AnnouncementCard, FilterBar],
  providers: [AnnouncementSearch],
  selector: 'app-announcement-list',
  styleUrl: './announcement-list.css',
  templateUrl: './announcement-list.html',
})
export class AnnouncementList {
  protected readonly search = inject(AnnouncementSearch);
}
