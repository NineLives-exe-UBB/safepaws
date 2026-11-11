
import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Announcement, AnnouncementRequest } from '../../models/announcement.model';
import { Coordinates } from '../../models/map.model';
import { Photo } from '../../models/photo.model';
import { AnnouncementService } from '../../services/announcement.service';
import { LocationService } from '../../services/location.service';
import { AnnouncementForm } from './announcement-form';
import { HttpErrorResponse } from '@angular/common/http';

describe('AnnouncementForm', () => {
  let fixture: ComponentFixture<AnnouncementForm>;
  let component: AnnouncementForm;
  let compiled: HTMLElement;

  const locationServiceMock = {
    getCurrentLocation: vi.fn().mockResolvedValue(null),
  };

  beforeEach(async () => {
    locationServiceMock.getCurrentLocation.mockResolvedValue(null);

    await TestBed.configureTestingModule({
      imports: [AnnouncementForm],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: LocationService,
          useValue: locationServiceMock,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AnnouncementForm);
    component = fixture.componentInstance;
    compiled = fixture.nativeElement as HTMLElement;

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should show the page title', () => {
    expect(compiled.querySelector('h1')?.textContent).toContain(
      'Post an announcement',
    );
  });

  it('should have LOST selected by default', () => {
    const lostRadio = compiled.querySelector(
      'input[value="LOST"]',
    ) as HTMLInputElement;

    expect(lostRadio.checked).toBe(true);
  });

  it('should display the available species', () => {
    const species = compiled.querySelector('#species');

    expect(species).toBeTruthy();
    expect(species?.textContent).toContain('Cat');
    expect(species?.textContent).toContain('Dog');
    expect(species?.textContent).toContain('Other');
  });

  it('should include an optional pet name field', () => {
    const petName = compiled.querySelector(
      '#petName',
    ) as HTMLInputElement;

    expect(petName).toBeTruthy();
    expect(petName.required).toBe(false);
  });

  it('should start with an invalid form', () => {
    expect(component['form'].invalid).toBe(true);
  });

  it('should require the main announcement fields and coordinates', () => {
    const controls = component['form'].controls;

    expect(controls.species.hasError('required')).toBe(true);
    expect(controls.eventDate.hasError('required')).toBe(true);
    expect(controls.area.hasError('required')).toBe(true);
    expect(controls.contactName.hasError('required')).toBe(true);
    expect(controls.contactPhone.hasError('required')).toBe(true);
    expect(controls.latitude.hasError('required')).toBe(true);
    expect(controls.longitude.hasError('required')).toBe(true);
  });

  it('should keep breed and color optional', () => {
    const controls = component['form'].controls;

    expect(controls.breed.valid).toBe(true);
    expect(controls.color.valid).toBe(true);
  });

  it('should accept a color with a comma', () => {
    const color = component['form'].controls.color;

    color.setValue('Grey, white paws');

    expect(color.valid).toBe(true);
  });


  it('should accept the same text as the backend in the optional fields', () => {
    const controls = component['form'].controls;

    controls.petName.setValue('Max 2');
    controls.breed.setValue('Mix (50/50)');
    controls.color.setValue('Black & white, 2 white paws');
    controls.contactName.setValue("Ana-Maria O'Brien 2");

    expect(controls.petName.valid).toBe(true);
    expect(controls.breed.valid).toBe(true);
    expect(controls.color.valid).toBe(true);
    expect(controls.contactName.valid).toBe(true);
  });

  it('should reject an event date in the future', () => {
    const eventDate = component['form'].controls.eventDate;

    eventDate.setValue('2999-01-01T10:00');

    expect(eventDate.hasError('futureDate')).toBe(true);

    eventDate.setValue('2026-10-30T18:00');

    expect(eventDate.valid).toBe(true);
  });

  it('should reject blank required text', () => {
    const controls = component['form'].controls;

    controls.area.setValue('   ');
    controls.contactName.setValue('   ');

    expect(controls.area.valid).toBe(false);
    expect(controls.contactName.valid).toBe(false);
  });

  it('should allow the pet name to be empty', () => {
    expect(component['form'].controls.petName.valid).toBe(true);
  });

  it('should reject a pet name longer than 50 characters', () => {
    component['form'].controls.petName.setValue('A'.repeat(51));

    expect(
      component['form'].controls.petName.hasError('maxlength'),
    ).toBe(true);
  });

  it('should reject invalid phone numbers', () => {
    const phone = component['form'].controls.contactPhone;

    phone.setValue('07123 45678');
    expect(phone.valid).toBe(false);

    phone.setValue('0712345678');
    expect(phone.valid).toBe(true);

    phone.setValue('+40712345678');
    expect(phone.valid).toBe(true);
  });

  it('should require a location to be selected on the map', () => {
    const controls = component['form'].controls;

    expect(controls.latitude.hasError('required')).toBe(true);
    expect(controls.longitude.hasError('required')).toBe(true);

    const location: Coordinates = {
      latitude: 46.7712,
      longitude: 23.6236,
    };

    component['onLocationSelected'](location);

    expect(controls.latitude.value).toBe(46.7712);
    expect(controls.longitude.value).toBe(23.6236);
    expect(controls.latitude.valid).toBe(true);
    expect(controls.longitude.valid).toBe(true);
    expect(component['selectedLocation']()).toEqual(location);
  });


  it('should lock every section and the map while submitting', () => {
    component['submitting'].set(true);
    fixture.detectChanges();

    const fieldsets: HTMLFieldSetElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('fieldset'),
    );
    expect(fieldsets.length).toBeGreaterThan(0);
    expect(fieldsets.every((fieldset) => fieldset.disabled)).toBe(true);

    component['onLocationSelected']({ latitude: 46.7712, longitude: 23.6236 });
    expect(component['selectedLocation']()).toBeNull();
  });

  it('should create a marker for the selected location', () => {
    component['onLocationSelected']({
      latitude: 46.7712,
      longitude: 23.6236,
    });

    expect(component['locationPins']()).toEqual([
      {
        latitude: 46.7712,
        longitude: 23.6236,
        type: 'LOST',
        label: 'Selected location',
      },
    ]);
  });

  it('should update the map center when geolocation succeeds', async () => {
    const location: Coordinates = {
      latitude: 46.7712,
      longitude: 23.6236,
    };

    locationServiceMock.getCurrentLocation.mockResolvedValue(location);

    const newFixture = TestBed.createComponent(AnnouncementForm);
    const newComponent = newFixture.componentInstance;

    newFixture.detectChanges();
    await newFixture.whenStable();

    expect(newComponent['mapCenter']()).toEqual(location);

    newFixture.destroy();
  });

    it('should keep the filled-in details when switching announcement type', () => {
    const controls = component['form'].controls;
    controls.species.setValue('CAT');
    controls.petName.setValue('Luna');
    controls.contactPhone.setValue('0712345678');
    component['onLocationSelected']({ latitude: 46.7712, longitude: 23.6236 });
    component['selectedFiles'].set([new File(['photo'], 'cat.png', { type: 'image/png' })]);

    controls.type.setValue('FOUND');

    expect(controls.species.value).toBe('CAT');
    expect(controls.petName.value).toBe('Luna');
    expect(controls.contactPhone.value).toBe('0712345678');
    expect(component['selectedLocation']()).toEqual({ latitude: 46.7712, longitude: 23.6236 });
    expect(component['selectedFiles']()).toHaveLength(1);
    expect(component['locationPins']()[0].type).toBe('FOUND');
  });

  it('should remove a selected photo and its preview URL', () => {
    const firstUrl = 'blob:first';
    const secondUrl = 'blob:second';

    component['selectedFiles'].set([
      new File(['photo1'], 'cat1.png', { type: 'image/png' }),
      new File(['photo2'], 'cat2.png', { type: 'image/png' }),
    ]);

    component['previewUrls'].set([firstUrl, secondUrl]);

    const revokeSpy = vi.spyOn(URL, 'revokeObjectURL');

    component['removeFile'](0);

    expect(component['selectedFiles']()).toHaveLength(1);
    expect(component['selectedFiles']()[0].name).toBe('cat2.png');
    expect(component['previewUrls']()).toEqual([secondUrl]);
    expect(revokeSpy).toHaveBeenCalledWith(firstUrl);

    revokeSpy.mockRestore();
  });

  it('should label the first selected photo as the main photo', () => {
    component['selectedFiles'].set([
      new File(['photo1'], 'cat1.png', { type: 'image/png' }),
      new File(['photo2'], 'cat2.png', { type: 'image/png' }),
    ]);

    component['previewUrls'].set([
      'blob:first',
      'blob:second',
    ]);

    fixture.detectChanges();

    expect(compiled.querySelector('.main-photo-label')?.textContent).toContain(
      'Main photo',
    );
  });

  describe('submitting', () => {
    const created = { id: 15 } as Announcement;
    const photo = new File(['photo'], 'cat.png', {
      type: 'image/png',
    });

    let sentAnnouncements: AnnouncementRequest[];
    let uploads: number[];

    beforeEach(() => {
      sentAnnouncements = [];
      uploads = [];

      const service = TestBed.inject(AnnouncementService);

      service.create = (announcement) => {
        sentAnnouncements.push(announcement);
        return of(created);
      };

      service.uploadPhotos = (id) => {
        uploads.push(id);
        return of([] as Photo[]);
      };

      component['form'].patchValue({
        species: 'CAT',
        eventDate: '2026-10-30T18:00',
        latitude: 46.7712,
        longitude: 23.6236,
        area: ' Mănăștur ',
        contactName: 'Ana Pop',
        contactPhone: '0712345678',
      });
    });

    it('should trim text and omit empty optional fields', () => {
      component['onSubmit']();

      const sent = sentAnnouncements[0];

      expect(sent.area).toBe('Mănăștur');
      expect(sent.petName).toBeUndefined();
      expect(sent.breed).toBeUndefined();
      expect(sent.color).toBeUndefined();
      expect(sent.distinctiveMarks).toBeUndefined();
    });

    it('should include the selected coordinates in the request', () => {
      component['onSubmit']();

      const sent = sentAnnouncements[0];

      expect(sent.latitude).toBe(46.7712);
      expect(sent.longitude).toBe(23.6236);
    });

    it('should not send an invalid form', () => {
      component['form'].controls.area.setValue('');

      component['onSubmit']();

      expect(sentAnnouncements).toHaveLength(0);
    });

    it('should upload the selected photos to the new announcement', () => {
      component['selectedFiles'].set([photo]);

      component['onSubmit']();

      expect(uploads).toEqual([15]);
      expect(component['successMessage']()).toContain(
        'uploaded successfully',
      );
    });

    it('should show an error when the announcement cannot be created', () => {
      TestBed.inject(AnnouncementService).create = () =>
        throwError(() => new Error('failed'));

      component['onSubmit']();

      expect(component['errorMessage']()).toContain('Could not create');
      expect(component['submitting']()).toBe(false);
    });


    it('should show the backend validation errors next to their fields', () => {
      TestBed.inject(AnnouncementService).create = () =>
        throwError(
          () =>
            new HttpErrorResponse({
              status: 400,
              error: {
                fieldErrors: [
                  { field: 'contactPhone', message: 'Contact phone must be a valid phone number' },
                  { field: 'coordinates', message: 'Latitude and longitude must be provided together' },
                ],
              },
            }),
        );

      component['onSubmit']();
      fixture.detectChanges();

      expect(component['serverError']('contactPhone')).toBe('Contact phone must be a valid phone number');
      expect(component['serverError']('latitude')).toBe('Latitude and longitude must be provided together');
      expect(compiled.textContent).toContain('Contact phone must be a valid phone number');
      expect(component['errorMessage']()).toContain('check the highlighted fields');
    });

    it('should clear a backend error when the field is changed', () => {
      TestBed.inject(AnnouncementService).create = () =>
        throwError(
          () =>
            new HttpErrorResponse({
              status: 400,
              error: { fieldErrors: [{ field: 'area', message: 'Area is required' }] },
            }),
        );

      component['onSubmit']();
      component['form'].controls.area.setValue('Mărăști, Cluj-Napoca');

      expect(component['serverError']('area')).toBeNull();
    });

    it('should not create a second announcement when retrying a failed photo upload', () => {
      const service = TestBed.inject(AnnouncementService);
      const workingUpload = service.uploadPhotos;

      service.uploadPhotos = () =>
        throwError(() => new Error('failed'));

      component['selectedFiles'].set([photo]);

      component['onSubmit']();

      expect(component['errorMessage']()).toContain(
        'could not be uploaded',
      );

      service.uploadPhotos = workingUpload;

      component['onSubmit']();

      expect(sentAnnouncements).toHaveLength(1);
      expect(uploads).toEqual([15]);
    });
  });
});
