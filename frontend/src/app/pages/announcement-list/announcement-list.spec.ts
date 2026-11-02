import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { Announcement } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';
import { MOCK_ANNOUNCEMENTS } from '../../services/mock-announcements';
import { AnnouncementList } from './announcement-list';

describe('AnnouncementList', () => {
  function stubService(response: Observable<Announcement[]>): AnnouncementService {
    const service = TestBed.runInInjectionContext(() => new AnnouncementService());
    service.getAll = () => response;
    return service;
  }

  async function render(
    response: Observable<Announcement[]>,
  ): Promise<ComponentFixture<AnnouncementList>> {
    await TestBed.configureTestingModule({
      imports: [AnnouncementList],
      providers: [
        provideHttpClient(),
        { provide: AnnouncementService, useFactory: () => stubService(response) },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AnnouncementList);
    await fixture.whenStable();
    return fixture;
  }

  it('should show the page title', async () => {
    const fixture = await render(of([]));
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('h1')?.textContent).toContain('Announcements');
  });

  it('should show a card for each announcement', async () => {
    const fixture = await render(of(MOCK_ANNOUNCEMENTS));
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelectorAll('app-announcement-card').length).toBe(
      MOCK_ANNOUNCEMENTS.length,
    );
    expect(compiled.querySelector('.empty')).toBeNull();
  });

  it('should show a message when there are no announcements', async () => {
    const fixture = await render(of([]));
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.empty')?.textContent).toContain('no announcements');
    expect(compiled.querySelector('app-announcement-card')).toBeNull();
  });

  it('should show an error message when loading fails', async () => {
    const fixture = await render(throwError(() => new Error('Server error')));
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.error')?.textContent).toContain('Could not load');
    expect(compiled.querySelector('app-announcement-card')).toBeNull();
  });
});
