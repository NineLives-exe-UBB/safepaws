import {
  AnnouncementFilters,
  DEFAULT_FILTERS,
  fromQueryParams,
  hasActiveFilters,
  needsLocation,
  toQueryParams,
  withoutLocationFilters,
} from './announcement-filters.model';

describe('announcement filters', () => {
  const fromUrl = (query: Record<string, string>) => fromQueryParams((name) => query[name] ?? null);

  describe('hasActiveFilters', () => {
    it('should be false for the defaults', () => {
      expect(hasActiveFilters(DEFAULT_FILTERS)).toBe(false);
    });

    it.each([
      { type: 'LOST' },
      { species: 'CAT' },
      { status: 'RESOLVED' },
      { color: 'grey' },
      { breed: 'European' },
      { area: 'Mănăștur' },
      { radiusKm: 5 },
    ] as Partial<AnnouncementFilters>[])('should be true when %o is set', (change) => {
      expect(hasActiveFilters({ ...DEFAULT_FILTERS, ...change })).toBe(true);
    });

    it('should ignore the sort order', () => {
      expect(hasActiveFilters({ ...DEFAULT_FILTERS, sort: 'oldest' })).toBe(false);
    });
  });

  it('should show active announcements by default, as the backend does', () => {
    expect(DEFAULT_FILTERS.status).toBe('ACTIVE');
    expect(DEFAULT_FILTERS.sort).toBe('newest');
  });

  describe('needsLocation', () => {
    it('should be true for a radius or for sorting by distance', () => {
      expect(needsLocation({ ...DEFAULT_FILTERS, radiusKm: 5 })).toBe(true);
      expect(needsLocation({ ...DEFAULT_FILTERS, sort: 'closest' })).toBe(true);
    });

    it('should be false for the other filters', () => {
      expect(needsLocation({ ...DEFAULT_FILTERS, species: 'CAT', sort: 'oldest' })).toBe(false);
    });
  });

  describe('toQueryParams', () => {
    it('should leave the URL empty for the defaults', () => {
      expect(toQueryParams(DEFAULT_FILTERS)).toEqual({});
    });

    it('should include the status when it is not the default', () => {
      expect(toQueryParams({ ...DEFAULT_FILTERS, status: 'RESOLVED' })).toEqual({
        status: 'RESOLVED',
      });
    });

    it('should only include the values that are set', () => {
      expect(
        toQueryParams({
          ...DEFAULT_FILTERS,
          species: 'CAT',
          color: 'grey',
          radiusKm: 10,
          sort: 'closest',
        }),
      ).toEqual({ species: 'CAT', color: 'grey', radiusKm: '10', sort: 'closest' });
    });
  });

  describe('fromQueryParams', () => {
    it('should return the defaults for an empty URL', () => {
      expect(fromUrl({})).toEqual(DEFAULT_FILTERS);
    });

    it('should read every filter', () => {
      expect(
        fromUrl({
          type: 'FOUND',
          species: 'DOG',
          status: 'RESOLVED',
          color: ' grey ',
          breed: 'Labrador',
          area: 'Centru',
          radiusKm: '25',
          sort: 'oldest',
        }),
      ).toEqual({
        type: 'FOUND',
        species: 'DOG',
        status: 'RESOLVED',
        color: 'grey',
        breed: 'Labrador',
        area: 'Centru',
        radiusKm: 25,
        sort: 'oldest',
      });
    });

    it('should ignore values that are not allowed', () => {
      expect(
        fromUrl({ type: 'MISSING', species: 'cat', status: 'x', radiusKm: '7', sort: 'NEWEST' }),
      ).toEqual(DEFAULT_FILTERS);
    });

    it('should give back the same filters that were written to the URL', () => {
      const filters: AnnouncementFilters = {
        ...DEFAULT_FILTERS,
        type: 'LOST',
        area: 'Cluj',
        radiusKm: 5,
        sort: 'closest',
      };
      expect(fromUrl(toQueryParams(filters))).toEqual(filters);
    });
  });

  describe('withoutLocationFilters', () => {
    it('should remove the radius and the distance sorting', () => {
      expect(
        withoutLocationFilters({
          ...DEFAULT_FILTERS,
          species: 'CAT',
          radiusKm: 5,
          sort: 'closest',
        }),
      ).toEqual({ ...DEFAULT_FILTERS, species: 'CAT' });
    });

    it('should keep the other sort orders', () => {
      expect(withoutLocationFilters({ ...DEFAULT_FILTERS, sort: 'oldest' }).sort).toBe('oldest');
    });
  });
});
