import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { AnnouncementSummary } from '../models/announcement.model';
import {
  AnnouncementFilters,
  DEFAULT_FILTERS,
  UserLocation,
} from '../models/announcement-filters.model';
import { MockAnnouncementService } from './mock-announcement.service';
import { MOCK_ANNOUNCEMENTS } from './mock-announcements';

describe('MockAnnouncementService', () => {
  const clujNapoca: UserLocation = { latitude: 46.7712, longitude: 23.6236 };
  let service: MockAnnouncementService;

  function getAll(
    change: Partial<AnnouncementFilters> = {},
    location: UserLocation | null = null,
  ): AnnouncementSummary[] {
    let result: AnnouncementSummary[] = [];
    service
      .getAll({ ...DEFAULT_FILTERS, ...change }, location)
      .subscribe((announcements) => (result = announcements));
    return result;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), MockAnnouncementService] });
    service = TestBed.inject(MockAnnouncementService);
  });

  it('should return every announcement when no filter is set, newest event first', () => {
    const result = getAll();

    expect(result.length).toBe(MOCK_ANNOUNCEMENTS.length);
    expect(result.map((announcement) => announcement.id)).toEqual(
      [...MOCK_ANNOUNCEMENTS]
        .sort((a, b) => b.eventDate.localeCompare(a.eventDate))
        .map((a) => a.id),
    );
  });

  it('should return only the fields of the list response', () => {
    expect(Object.keys(getAll()[0]).sort()).toEqual(
      ['area', 'eventDate', 'id', 'petName', 'photos', 'species', 'status', 'type'].sort(),
    );
  });

  it('should filter by type, species and status', () => {
    expect(getAll({ type: 'FOUND' }).every((a) => a.type === 'FOUND')).toBe(true);
    expect(getAll({ species: 'CAT' }).every((a) => a.species === 'CAT')).toBe(true);
    expect(getAll({ status: 'RESOLVED' }).map((a) => a.id)).toEqual([3]);
  });

  it('should filter by color, breed and area without caring about case', () => {
    expect(getAll({ color: 'GREY' }).length).toBeGreaterThan(0);
    expect(getAll({ breed: 'labrador' }).map((a) => a.id)).toEqual([2]);
    expect(getAll({ area: 'floresti' }).map((a) => a.id)).toEqual([2]);
  });

  it('should combine the filters', () => {
    expect(getAll({ species: 'CAT', type: 'FOUND' }).map((a) => a.id)).toEqual([5]);
    expect(getAll({ species: 'OTHER', type: 'FOUND' })).toEqual([]);
  });

  it('should sort the oldest event first', () => {
    const ids = getAll({ sort: 'OLDEST' }).map((a) => a.id);

    expect(ids[0]).toBe(1);
    expect(ids[ids.length - 1]).toBe(8);
  });

  it('should only add the distance when the location is known', () => {
    expect(getAll()[0].distanceKm).toBeUndefined();
    expect(getAll({}, clujNapoca)[0].distanceKm).toBeGreaterThan(0);
  });

  it('should calculate the distance in kilometers', () => {
    const centru = getAll({}, clujNapoca).find((a) => a.id === 3);
    const bucharest = getAll({}, clujNapoca).find((a) => a.id === 1);

    expect(centru?.distanceKm).toBeCloseTo(0, 1);
    expect(bucharest?.distanceKm).toBeGreaterThan(300);
    expect(bucharest?.distanceKm).toBeLessThan(330);
  });

  it('should keep the announcements inside the radius', () => {
    const ids = getAll({ radiusKm: 5 }, clujNapoca).map((a) => a.id);

    expect(ids).toContain(3);
    expect(ids).not.toContain(1);
    expect(ids).not.toContain(2);
  });

  it('should leave out every announcement when a radius is given without a location', () => {
    expect(getAll({ radiusKm: 50 })).toEqual([]);
  });

  it('should sort the nearest first', () => {
    const distances = getAll({ sort: 'NEAREST' }, clujNapoca).map((a) => a.distanceKm ?? 0);

    expect(distances).toEqual([...distances].sort((a, b) => a - b));
  });

  it('should use the photo url as it is', () => {
    expect(service.photoUrl({ id: 1, url: '/mock-photos/cat.svg', isMain: true })).toBe(
      '/mock-photos/cat.svg',
    );
  });
});
