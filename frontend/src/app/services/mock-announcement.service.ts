import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { AnnouncementSummary } from '../models/announcement.model';
import {
  AnnouncementFilters,
  DEFAULT_FILTERS,
  UserLocation,
} from '../models/announcement-filters.model';
import { Photo } from '../models/photo.model';
import { AnnouncementService } from './announcement.service';
import { MOCK_ANNOUNCEMENTS, MockAnnouncement } from './mock-announcements';

const EARTH_RADIUS_KM = 6371;

/** Filters and sorts the mock data the way the backend is expected to, until it can do it. */
@Injectable()
export class MockAnnouncementService extends AnnouncementService {
  override getAll(
    filters: AnnouncementFilters = DEFAULT_FILTERS,
    location: UserLocation | null = null,
  ): Observable<AnnouncementSummary[]> {
    const results = MOCK_ANNOUNCEMENTS.map((mock) => this.toSummary(mock, location))
      .filter(({ mock, summary }) => this.matches(mock, summary, filters))
      .sort((first, second) => this.compare(first.summary, second.summary, filters));
    return of(results.map(({ summary }) => summary));
  }

  override photoUrl(photo: Photo): string {
    return photo.url;
  }

  private toSummary(
    mock: MockAnnouncement,
    location: UserLocation | null,
  ): { mock: MockAnnouncement; summary: AnnouncementSummary } {
    const { color, breed, latitude, longitude, ...summary } = mock;
    const distanceKm = location ? this.distanceKm(location, mock) : undefined;
    return { mock, summary: distanceKm === undefined ? summary : { ...summary, distanceKm } };
  }

  private matches(
    mock: MockAnnouncement,
    summary: AnnouncementSummary,
    filters: AnnouncementFilters,
  ): boolean {
    const contains = (value: string, search: string) =>
      value.toLowerCase().includes(search.toLowerCase());
    return (
      (!filters.type || mock.type === filters.type) &&
      (!filters.species || mock.species === filters.species) &&
      (!filters.status || mock.status === filters.status) &&
      contains(mock.color, filters.color) &&
      contains(mock.breed, filters.breed) &&
      contains(mock.area, filters.area) &&
      (filters.radiusKm === null ||
        (summary.distanceKm != null && summary.distanceKm <= filters.radiusKm))
    );
  }

  private compare(
    first: AnnouncementSummary,
    second: AnnouncementSummary,
    filters: AnnouncementFilters,
  ): number {
    const newestFirst = Date.parse(second.eventDate) - Date.parse(first.eventDate);
    if (filters.sort === 'OLDEST') {
      return -newestFirst;
    }
    if (filters.sort === 'NEAREST') {
      return (first.distanceKm ?? Infinity) - (second.distanceKm ?? Infinity);
    }
    return newestFirst;
  }

  private distanceKm(from: UserLocation, to: UserLocation): number {
    const toRadians = (degrees: number) => (degrees * Math.PI) / 180;
    const latitudeDifference = toRadians(to.latitude - from.latitude);
    const longitudeDifference = toRadians(to.longitude - from.longitude);
    const a =
      Math.sin(latitudeDifference / 2) ** 2 +
      Math.cos(toRadians(from.latitude)) *
        Math.cos(toRadians(to.latitude)) *
        Math.sin(longitudeDifference / 2) ** 2;
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
  }
}
