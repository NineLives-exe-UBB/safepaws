import { Component, computed, inject } from '@angular/core';
import { AnnouncementPopup } from '../../components/announcement-popup/announcement-popup';
import { FilterBar } from '../../components/filter-bar/filter-bar';
import { MapView } from '../../components/map/map';
import { AnnouncementSummary, SPECIES_LABELS } from '../../models/announcement.model';
import { MapPin } from '../../models/map.model';
import { AnnouncementSearch } from '../../services/announcement-search';

const USER_ZOOM = 13;

type Mappable = AnnouncementSummary & { latitude: number; longitude: number };

@Component({
  imports: [AnnouncementPopup, FilterBar, MapView],
  providers: [AnnouncementSearch],
  selector: 'app-map-page',
  styleUrl: './map-page.css',
  templateUrl: './map-page.html',
})
export class MapPage {
  protected readonly search = inject(AnnouncementSearch);
  protected readonly userZoom = USER_ZOOM;

  private readonly mappable = computed(() =>
    this.search
      .announcements()
      .filter((a): a is Mappable => a.latitude !== null && a.longitude !== null),
  );

  protected readonly pins = computed<MapPin[]>(() =>
    this.mappable().map((announcement) => ({
      id: announcement.id,
      latitude: announcement.latitude,
      longitude: announcement.longitude,
      type: announcement.type,
      label: announcement.petName ?? SPECIES_LABELS[announcement.species],
    })),
  );

  /** Announcements that match the filters but have no position, so they cannot get a pin. */
  protected readonly withoutLocationCount = computed(
    () => this.search.announcements().length - this.mappable().length,
  );

  protected announcementFor(pin: MapPin): AnnouncementSummary | undefined {
    return this.search.announcements().find((announcement) => announcement.id === pin.id);
  }
}
