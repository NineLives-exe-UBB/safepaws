import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MockAnnouncementService } from './mock-announcement.service';
import { MOCK_ANNOUNCEMENTS } from './mock-announcements';

describe('MockAnnouncementService', () => {
  let service: MockAnnouncementService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), MockAnnouncementService],
    });
    service = TestBed.inject(MockAnnouncementService);
  });

  it('should return the mock announcements', () => {
    let result = [] as unknown[];
    service.getAll().subscribe((announcements) => (result = announcements));

    expect(result).toEqual(MOCK_ANNOUNCEMENTS);
  });

  it('should use the photo url as it is', () => {
    expect(service.photoUrl({ id: 1, url: '/mock-photos/cat.svg', isMain: true })).toBe(
      '/mock-photos/cat.svg',
    );
  });
});
