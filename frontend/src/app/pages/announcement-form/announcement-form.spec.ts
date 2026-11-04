import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Announcement, AnnouncementRequest } from '../../models/announcement.model';
import { Photo } from '../../models/photo.model';
import { AnnouncementService } from '../../services/announcement.service';
import { AnnouncementForm } from './announcement-form';

describe('AnnouncementForm', () => {
  let fixture: ComponentFixture<AnnouncementForm>;
  let component: AnnouncementForm;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnnouncementForm],
      providers: [provideHttpClient()],
    }).compileComponents();

    fixture = TestBed.createComponent(AnnouncementForm);
    component = fixture.componentInstance;
    compiled = fixture.nativeElement as HTMLElement;

    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should show the page title', () => {
    expect(compiled.querySelector('h1')?.textContent).toContain('Post an announcement');
  });

  it('should have LOST selected by default', () => {
    const lostRadio = compiled.querySelector('input[value="LOST"]') as HTMLInputElement;

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
    const petName = compiled.querySelector('#petName') as HTMLInputElement;

    expect(petName).toBeTruthy();
    expect(petName.required).toBe(false);
  });

  it('should start with an invalid form', () => {
    expect(component['form'].invalid).toBe(true);
  });

  it('should require the main announcement fields', () => {
    const controls = component['form'].controls;

    expect(controls.species.hasError('required')).toBe(true);
    expect(controls.eventDate.hasError('required')).toBe(true);
    expect(controls.area.hasError('required')).toBe(true);
    expect(controls.contactName.hasError('required')).toBe(true);
    expect(controls.contactPhone.hasError('required')).toBe(true);
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

    expect(component['form'].controls.petName.hasError('maxlength')).toBe(true);
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

  it('should reject coordinates when only latitude is provided', () => {
    component['form'].controls.latitude.setValue(46.7712);

    expect(component['form'].hasError('coordinatesTogether')).toBe(true);
  });

  it('should accept coordinates when both are provided', () => {
    component['form'].controls.latitude.setValue(46.7712);
    component['form'].controls.longitude.setValue(23.6236);

    expect(component['form'].hasError('coordinatesTogether')).toBe(false);
  });

  it('should accept coordinates when both are empty', () => {
    expect(component['form'].hasError('coordinatesTogether')).toBe(false);
  });

  it('should reset the form when switching announcement type', () => {
    const controls = component['form'].controls;

    controls.species.setValue('CAT');
    controls.petName.setValue('Luna');
    controls.breed.setValue('European');
    controls.color.setValue('Grey');
    controls.contactName.setValue('Ana');
    controls.contactPhone.setValue('0712345678');

    controls.species.markAsTouched();
    component['selectedFiles'].set([new File(['photo'], 'cat.png', { type: 'image/png' })]);
    component['errorMessage'].set('Previous error');
    component['successMessage'].set('Previous success');

    controls.type.setValue('FOUND');
    component['onTypeChange']();

    expect(controls.type.value).toBe('FOUND');
    expect(controls.species.value).toBe('');
    expect(controls.petName.value).toBe('');
    expect(controls.breed.value).toBe('');
    expect(controls.color.value).toBe('');
    expect(controls.contactName.value).toBe('');
    expect(controls.contactPhone.value).toBe('');

    expect(controls.species.touched).toBe(false);
    expect(component['selectedFiles']()).toHaveLength(0);
    expect(component['errorMessage']()).toBe('');
    expect(component['successMessage']()).toBe('');
  });

  it('should remove a selected photo', () => {
    component['selectedFiles'].set([
      new File(['photo1'], 'cat1.png', { type: 'image/png' }),
      new File(['photo2'], 'cat2.png', { type: 'image/png' }),
    ]);

    component['removeFile'](0);

    expect(component['selectedFiles']()).toHaveLength(1);
    expect(component['selectedFiles']()[0].name).toBe('cat2.png');
  });

  describe('submitting', () => {
    const created = { id: 15 } as Announcement;
    const photo = new File(['photo'], 'cat.png', { type: 'image/png' });
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
        area: ' Mănăștur ',
        contactName: 'Ana Pop',
        contactPhone: '0712345678',
      });
    });

    it('should send empty optional fields as missing, not as empty text', () => {
      component['onSubmit']();

      const sent = sentAnnouncements[0];
      expect(sent.area).toBe('Mănăștur');
      expect(sent.petName).toBeUndefined();
      expect(sent.breed).toBeUndefined();
      expect(sent.color).toBeUndefined();
      expect(sent.distinctiveMarks).toBeUndefined();
      expect(sent.latitude).toBeUndefined();
      expect(sent.longitude).toBeUndefined();
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
      expect(component['successMessage']()).toContain('uploaded successfully');
    });

    it('should show an error when the announcement cannot be created', () => {
      TestBed.inject(AnnouncementService).create = () => throwError(() => new Error('failed'));

      component['onSubmit']();

      expect(component['errorMessage']()).toContain('Could not create');
      expect(component['submitting']()).toBe(false);
    });

    it('should not create a second announcement when retrying a failed photo upload', () => {
      const service = TestBed.inject(AnnouncementService);
      const workingUpload = service.uploadPhotos;
      service.uploadPhotos = () => throwError(() => new Error('failed'));
      component['selectedFiles'].set([photo]);

      component['onSubmit']();
      expect(component['errorMessage']()).toContain('could not be uploaded');

      service.uploadPhotos = workingUpload;
      component['onSubmit']();

      expect(sentAnnouncements).toHaveLength(1);
      expect(uploads).toEqual([15]);
    });
  });
});
