import { AnnouncementSummary, Species } from '../models/announcement.model';

/** What the backend knows about an announcement; the list only returns part of it. */
export interface MockAnnouncement extends AnnouncementSummary {
  color: string;
  breed: string;
  latitude: number;
  longitude: number;
}

const photo = (id: number, animal: 'cat' | 'dog') => [
  { id, url: `/mock-photos/${animal}.svg`, isMain: true },
];

const mock = (
  id: number,
  type: 'LOST' | 'FOUND',
  species: Species,
  petName: string | null,
  area: string,
  eventDate: string,
  details: Pick<MockAnnouncement, 'color' | 'breed' | 'latitude' | 'longitude'> &
    Partial<Pick<MockAnnouncement, 'status' | 'photos'>>,
): MockAnnouncement => ({
  id,
  type,
  species,
  petName,
  area,
  eventDate,
  status: 'ACTIVE',
  photos: [],
  ...details,
});

export const MOCK_ANNOUNCEMENTS: MockAnnouncement[] = [
  mock(8, 'LOST', 'CAT', 'Mitzi', 'Mănăștur, Cluj-Napoca', '2026-11-03T18:00:00', {
    color: 'Grey with white paws',
    breed: 'European',
    latitude: 46.755,
    longitude: 23.556,
    photos: photo(1, 'cat'),
  }),
  mock(7, 'FOUND', 'DOG', null, 'Zorilor, Cluj-Napoca', '2026-11-02T16:45:00', {
    color: 'White with brown spots',
    breed: 'Mixed breed',
    latitude: 46.755,
    longitude: 23.58,
    photos: photo(2, 'dog'),
  }),
  mock(6, 'LOST', 'DOG', 'Rex', 'Gheorgheni, Cluj-Napoca', '2026-11-01T07:30:00', {
    color: 'Black and tan',
    breed: 'German Shepherd',
    latitude: 46.77,
    longitude: 23.63,
    photos: photo(3, 'dog'),
  }),
  mock(5, 'FOUND', 'CAT', null, 'Mărăști, Cluj-Napoca', '2026-10-31T12:00:00', {
    color: 'Orange tabby',
    breed: 'European',
    latitude: 46.78,
    longitude: 23.615,
    photos: photo(4, 'cat'),
  }),
  mock(4, 'LOST', 'OTHER', 'Kiwi', 'Grigorescu, Cluj-Napoca', '2026-10-27T10:00:00', {
    color: 'Green and yellow',
    breed: 'Budgerigar',
    latitude: 46.77,
    longitude: 23.56,
  }),
  mock(3, 'LOST', 'CAT', 'Luna', 'Centru, Cluj-Napoca', '2026-10-25T21:00:00', {
    color: 'Cream with dark brown face',
    breed: 'Siamese',
    latitude: 46.7712,
    longitude: 23.6236,
    status: 'RESOLVED',
  }),
  mock(2, 'FOUND', 'DOG', null, 'Floresti', '2026-10-24T09:00:00', {
    color: 'Golden',
    breed: 'Labrador Retriever',
    latitude: 46.7485,
    longitude: 23.4985,
  }),
  mock(1, 'LOST', 'CAT', 'Tom', 'Bucharest', '2026-10-20T20:00:00', {
    color: 'Grey',
    breed: 'British Shorthair',
    latitude: 44.4268,
    longitude: 26.1025,
  }),
];
