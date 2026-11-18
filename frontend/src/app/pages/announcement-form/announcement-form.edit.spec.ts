import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import type { Mock } from 'vitest';

import { Announcement, AnnouncementRequest } from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';
import { AuthService } from '../../services/auth.service';
import { LocationService } from '../../services/location.service';
import { AnnouncementForm } from './announcement-form';

describe('AnnouncementForm (edit)', () => {
  const announcement: Announcement = {
    id: 9,
    ownerId: 5,
    type: 'FOUND',
    species: 'DOG',
    petName: 'Rex',
    breed: null,
    color: 'Brown',
    distinctiveMarks: null,
    eventDate: '2026-10-30T18:00:00',
    latitude: 46.7712,
    longitude: 23.6236,
    area: 'Mănăștur, Cluj-Napoca',
    contactName: 'Ana Pop',
    contactPhone: '0712345678',
    status: 'ACTIVE',
    createdAt: '2026-11-01T14:30:00',
    photos: [],
  };

  const currentUser = signal<{ id: number; email: string; displayName: string } | null>(null);

  let getById: Mock<(id: number) => Observable<Announcement>>;
  let update: Mock<(id: number, request: AnnouncementRequest) => Observable<Announcement>>;
  let create: Mock;
  let fixture: ComponentFixture<AnnouncementForm>;
  let element: HTMLElement;

  async function render(): Promise<void> {
    TestBed.configureTestingModule({
      imports: [AnnouncementForm],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { user: currentUser } },
        { provide: LocationService, useValue: { getCurrentLocation: vi.fn().mockResolvedValue(null) } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '9' }) } },
        },
        { provide: AnnouncementService, useValue: { getById, update, create, uploadPhotos: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(AnnouncementForm);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  }

  const input = (id: string) => element.querySelector<HTMLInputElement>(`#${id}`)!;
  const submit = () => element.querySelector<HTMLButtonElement>('.submit-button')!;

  beforeEach(() => {
    vi.useRealTimers();
    currentUser.set({ id: 5, email: 'owner@example.com', displayName: 'Test Owner' });
    getById = vi.fn(() => of(announcement));
    update = vi.fn(() => of(announcement));
    create = vi.fn();
  });

  it('loads the announcement of the URL into the form', async () => {
    await render();

    expect(getById).toHaveBeenCalledWith(9);
    expect(element.querySelector('h1')?.textContent).toContain('Edit announcement');
    expect(input('petName').value).toBe('Rex');
    expect(input('area').value).toBe('Mănăștur, Cluj-Napoca');
    expect(input('eventDate').value).toBe('2026-10-30T18:00');
    expect(element.querySelector<HTMLInputElement>('input[value="FOUND"]')!.checked).toBe(true);
    expect(submit().textContent?.trim()).toBe('Save changes');
  });

  it('keeps the contact name of the announcement, not the display name', async () => {
    await render();

    expect(input('contactName').value).toBe('Ana Pop');
  });

  it('saves with PUT on the same announcement, never creates a new one', async () => {
    await render();

    input('petName').value = 'Rexy';
    input('petName').dispatchEvent(new Event('input'));
    submit().click();
    fixture.detectChanges();

    expect(create).not.toHaveBeenCalled();
    expect(update).toHaveBeenCalledTimes(1);
    const [id, request] = update.mock.calls[0];
    expect(id).toBe(9);
    expect(request.petName).toBe('Rexy');
    expect(request.type).toBe('FOUND');
    expect(request.latitude).toBe(46.7712);
    expect(element.textContent).toContain('Your changes were saved.');
  });

  it('does not let another user edit the announcement', async () => {
    currentUser.set({ id: 8, email: 'other@example.com', displayName: 'Other' });

    await render();

    expect(element.textContent).toContain('You can only change your own announcements.');
    expect(submit().disabled).toBe(false);
    submit().click();
    expect(update).not.toHaveBeenCalled();
  });

  it('says so when the backend refuses with 403', async () => {
    update = vi.fn(() => throwError(() => new HttpErrorResponse({ status: 403 })));

    await render();
    submit().click();
    fixture.detectChanges();

    expect(element.textContent).toContain('You can only change your own announcements.');
  });

  it('says so when the announcement does not exist', async () => {
    getById = vi.fn(() => throwError(() => new HttpErrorResponse({ status: 404 })));

    await render();

    expect(element.textContent).toContain('This announcement does not exist.');
    expect(update).not.toHaveBeenCalled();
  });

  it('does not mark a new photo as main when the announcement already has photos', async () => {
    getById = vi.fn(() => of({ ...announcement, photos: [{ id: 1, url: '/photos/1', isMain: true }] }));
    URL.createObjectURL = vi.fn(() => 'blob:preview');

    await render();
    const photoInput = input('photos');
    Object.defineProperty(photoInput, 'files', {
      value: [new File(['x'], 'new.jpg', { type: 'image/jpeg' })],
    });
    photoInput.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(element.querySelector('.photo-preview')).not.toBeNull();
    expect(element.querySelector('.main-photo-label')).toBeNull();
  });
});
