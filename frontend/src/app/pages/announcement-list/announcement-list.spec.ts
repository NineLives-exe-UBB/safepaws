import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { AnnouncementSummary } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';
import { AnnouncementList } from './announcement-list';

describe('AnnouncementList', () => {
  const announcements: AnnouncementSummary[] = [
    {
      id: 2,
      type: 'FOUND',
      petName: null,
      species: 'DOG',
      area: 'Centru, Cluj-Napoca',
      eventDate: '2026-10-29T09:15:00',
      status: 'ACTIVE',
      photos: [{ id: 3, url: '/photos/3', isMain: true }],
    },
    {
      id: 1,
      type: 'LOST',
      petName: 'Misi',
      species: 'CAT',
      area: 'Mănăștur, Cluj-Napoca',
      eventDate: '2026-10-30T18:00:00',
      status: 'ACTIVE',
      photos: [],
    },
  ];

  function stubService(response: Observable<AnnouncementSummary[]>): AnnouncementService {
    const service = TestBed.runInInjectionContext(() => new AnnouncementService());
    service.getAll = () => response;
    return service;
  }

  async function render(
    response: Observable<AnnouncementSummary[]>,
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
    const fixture = await render(of(announcements));
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelectorAll('app-announcement-card').length).toBe(announcements.length);
    expect(compiled.querySelector('.empty')).toBeNull();
  });

  it('should sort the cards by event date, newest first', async () => {
    const posted = (id: number, eventDate: string): AnnouncementSummary => ({
      ...announcements[0],
      id,
      eventDate,
    });
    const fixture = await render(
      of([
        posted(1, '2026-10-30T18:00:00'),
        posted(2, '2026-11-02T09:00:00'),
        posted(3, '2026-11-01T12:00:00'),
        posted(4, '2026-11-01T12:00:00'),
      ]),
    );
    const compiled = fixture.nativeElement as HTMLElement;
    const dates = Array.from(compiled.querySelectorAll('.date')).map((date) =>
      date.textContent?.trim(),
    );

    expect(dates).toEqual([
      '02 Nov 2026, 09:00',
      '01 Nov 2026, 12:00',
      '01 Nov 2026, 12:00',
      '30 Oct 2026, 18:00',
    ]);
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
