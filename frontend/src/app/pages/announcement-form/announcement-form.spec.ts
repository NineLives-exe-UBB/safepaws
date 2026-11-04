
import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
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
    expect(compiled.querySelector('h1')?.textContent)
      .toContain('Post an announcement');
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
    expect(controls.breed.hasError('required')).toBe(true);
    expect(controls.color.hasError('required')).toBe(true);
    expect(controls.eventDate.hasError('required')).toBe(true);
    expect(controls.area.hasError('required')).toBe(true);
    expect(controls.contactName.hasError('required')).toBe(true);
    expect(controls.contactPhone.hasError('required')).toBe(true);
  });

  it('should allow the pet name to be empty', () => {
    expect(component['form'].controls.petName.valid).toBe(true);
  });

  it('should reject a pet name longer than 50 characters', () => {
    component['form'].controls.petName.setValue('A'.repeat(51));

    expect(component['form'].controls.petName.hasError('maxlength'))
      .toBe(true);
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
    component['selectedFiles'].set([
      new File(['photo'], 'cat.png', { type: 'image/png' }),
    ]);
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
});
