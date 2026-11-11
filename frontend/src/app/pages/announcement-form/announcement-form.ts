
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AnnouncementRequest, Species } from '../../models/announcement.model';
import { Coordinates, MapPin } from '../../models/map.model';
import { AnnouncementService } from '../../services/announcement.service';
import { LocationService } from '../../services/location.service';
import { MapView } from '../../components/map/map';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  imports: [ReactiveFormsModule, RouterLink, MapView],
  selector: 'app-announcement-form',
  styleUrl: './announcement-form.css',
  templateUrl: './announcement-form.html',
})
export class AnnouncementForm implements OnInit, OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly announcementService = inject(AnnouncementService);
  private readonly locationService = inject(LocationService);
  private readonly router = inject(Router);

  protected readonly selectedFiles = signal<File[]>([]);
  protected readonly previewUrls = signal<string[]>([]);
  protected readonly submitting = signal(false);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');

  protected readonly mapCenter = signal<Coordinates | null>(null);
  protected readonly selectedLocation = signal<Coordinates | null>(null);

  private redirectTimeout?: ReturnType<typeof setTimeout>;
  private createdAnnouncementId?: number;

  private readonly notInFutureValidator: ValidatorFn = (
    control: AbstractControl,
  ): ValidationErrors | null =>
    control.value && new Date(control.value).getTime() > Date.now()
      ? { futureDate: true }
      : null;

  protected readonly form = this.formBuilder.group({
    type: ['LOST' as 'LOST' | 'FOUND', Validators.required],
    species: ['' as Species | '', Validators.required],

    petName: [
      '',
      [
        Validators.maxLength(50),
        Validators.pattern(/^[\p{L}\s'-]*$/u),
      ],
    ],

    breed: [
      '',
      [
        Validators.maxLength(100),
        Validators.pattern(/^[\p{L}\s'-]*$/u),
      ],
    ],

    color: [
      '',
      [
        Validators.maxLength(100),
        Validators.pattern(/^[\p{L}\s,'-]*$/u),
      ],
    ],

    distinctiveMarks: ['', Validators.maxLength(500)],

    eventDate: ['', [Validators.required, this.notInFutureValidator]],

    latitude: [
      null as number | null,
      [
        Validators.required,
        Validators.min(-90),
        Validators.max(90),
      ],
    ],

    longitude: [
      null as number | null,
      [
        Validators.required,
        Validators.min(-180),
        Validators.max(180),
      ],
    ],

    area: [
      '',
      [
        Validators.required,
        Validators.maxLength(150),
        Validators.pattern(/\S/),
      ],
    ],

    contactName: [
      '',
      [
        Validators.required,
        Validators.maxLength(100),
        Validators.pattern(/^\s*[\p{L}'-][\p{L}\s'-]*$/u),
      ],
    ],

    contactPhone: [
      '',
      [
        Validators.required,
        Validators.maxLength(20),
        Validators.pattern(/^\+?[0-9]{7,15}$/),
      ],
    ],
  });

    private readonly selectedType = toSignal(this.form.controls.type.valueChanges, {
    initialValue: this.form.controls.type.value,
  });

  ngOnInit(): void {
    this.locationService.getCurrentLocation().then((location) => {
      if (location) {
        this.mapCenter.set(location);
      }
    });
  }

  protected readonly locationPins = computed<MapPin[]>(() => {
    const location = this.selectedLocation();
    if (!location) {
      return [];
    }
    return [{ ...location, type: this.selectedType() ?? 'LOST', label: 'Selected location' }];
  });

  protected onLocationSelected(location: Coordinates): void {
    this.selectedLocation.set(location);

    this.form.controls.latitude.setValue(location.latitude);
    this.form.controls.longitude.setValue(location.longitude);

    this.form.controls.latitude.markAsTouched();
    this.form.controls.longitude.markAsTouched();

    this.form.controls.latitude.markAsDirty();
    this.form.controls.longitude.markAsDirty();
  }

  protected onTypeChange(): void {
    const selectedType = this.form.controls.type.value;

    this.clearPreviews();
    this.selectedLocation.set(null);

    this.form.reset({
      type: selectedType,
      species: '',
      petName: '',
      breed: '',
      color: '',
      distinctiveMarks: '',
      eventDate: '',
      latitude: null,
      longitude: null,
      area: '',
      contactName: '',
      contactPhone: '',
    });

    this.selectedFiles.set([]);
    this.createdAnnouncementId = undefined;
    this.successMessage.set('');
    this.errorMessage.set('');

    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);

    const allowedTypes = ['image/jpeg', 'image/png'];
    const maxSize = 5 * 1024 * 1024;

    const validFiles: File[] = [];
    const errors: string[] = [];

    for (const file of files) {
      if (!allowedTypes.includes(file.type)) {
        errors.push(
          `${file.name}: only JPG and PNG images are allowed.`,
        );
        continue;
      }

      if (file.size > maxSize) {
        errors.push(
          `${file.name}: the maximum file size is 5 MB.`,
        );
        continue;
      }

      const alreadySelected = [
        ...this.selectedFiles(),
        ...validFiles,
      ].some(
        (existing) =>
          existing.name === file.name &&
          existing.size === file.size &&
          existing.lastModified === file.lastModified,
      );

      if (!alreadySelected) {
        validFiles.push(file);
      }
    }

    const newPreviewUrls = validFiles.map((file) =>
      URL.createObjectURL(file),
    );

    this.selectedFiles.update((currentFiles) => [
      ...currentFiles,
      ...validFiles,
    ]);

    this.previewUrls.update((currentUrls) => [
      ...currentUrls,
      ...newPreviewUrls,
    ]);

    this.errorMessage.set(errors.join(' '));
    input.value = '';
  }

  protected removeFile(index: number): void {
    const urlToRemove = this.previewUrls()[index];

    if (urlToRemove) {
      URL.revokeObjectURL(urlToRemove);
    }

    this.selectedFiles.update((files) =>
      files.filter((_, i) => i !== index),
    );

    this.previewUrls.update((urls) =>
      urls.filter((_, i) => i !== index),
    );
  }

  private clearPreviews(): void {
    this.previewUrls().forEach((url) =>
      URL.revokeObjectURL(url),
    );

    this.previewUrls.set([]);
  }

  private redirectToAnnouncements(): void {
    if (this.redirectTimeout) {
      clearTimeout(this.redirectTimeout);
    }

    this.redirectTimeout = setTimeout(() => {
      this.router.navigate(['/announcements']);
    }, 700);
  }

  ngOnDestroy(): void {
    this.clearPreviews();

    if (this.redirectTimeout) {
      clearTimeout(this.redirectTimeout);
    }
  }

  protected onSubmit(): void {
    this.form.markAllAsTouched();

    if (this.form.invalid || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    // If the announcement already exists, retry only the photo upload.
    if (this.createdAnnouncementId !== undefined) {
      this.uploadSelectedPhotos(this.createdAnnouncementId);
      return;
    }

    const values = this.form.getRawValue();

    const announcement: AnnouncementRequest = {
      type: values.type!,
      species: values.species as Species,
      petName: this.optionalText(values.petName),
      breed: this.optionalText(values.breed),
      color: this.optionalText(values.color),
      distinctiveMarks: this.optionalText(values.distinctiveMarks),
      eventDate: values.eventDate!,
      latitude: values.latitude!,
      longitude: values.longitude!,
      area: values.area!.trim(),
      contactName: values.contactName!.trim(),
      contactPhone: values.contactPhone!.trim(),
    };

    this.announcementService.create(announcement).subscribe({
      next: (createdAnnouncement) => {
        this.createdAnnouncementId = createdAnnouncement.id;
        this.uploadSelectedPhotos(createdAnnouncement.id);
      },

      error: () => {
        this.submitting.set(false);

        this.errorMessage.set(
          'Could not create the announcement. Please check your information and try again.',
        );
      },
    });
  }

  private uploadSelectedPhotos(announcementId: number): void {
    const files = this.selectedFiles();

    if (files.length === 0) {
      this.submitting.set(false);

      this.successMessage.set(
        'Your announcement was created successfully!',
      );

      this.redirectToAnnouncements();
      return;
    }

    this.announcementService.uploadPhotos(announcementId, files).subscribe({
      next: () => {
        this.submitting.set(false);

        this.successMessage.set(
          'Your announcement and photos were uploaded successfully!',
        );

        this.redirectToAnnouncements();
      },

      error: () => {
        this.submitting.set(false);

        this.errorMessage.set(
          'Your announcement was created, but the photos could not be uploaded. Check the photos and submit again to retry, or remove them to continue without photos.',
        );
      },
    });
  }

  private optionalText(
    value: string | null | undefined,
  ): string | undefined {
    return value?.trim() || undefined;
  }
}
