
import { Component, inject, OnDestroy, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  AnnouncementRequest,
  Species,
} from '../../models/announcement.model';
import { AnnouncementService } from '../../services/announcement.service';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-announcement-form',
  styleUrl: './announcement-form.css',
  templateUrl: './announcement-form.html',
})
export class AnnouncementForm implements OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly announcementService = inject(AnnouncementService);
  private readonly router = inject(Router);

  protected readonly selectedFiles = signal<File[]>([]);
  protected readonly previewUrls = signal<string[]>([]);
  protected readonly submitting = signal(false);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');

  private redirectTimeout?: ReturnType<typeof setTimeout>;

  private readonly coordinatesValidator: ValidatorFn = (
    control: AbstractControl,
  ): ValidationErrors | null => {
    const latitude = control.get('latitude')?.value;
    const longitude = control.get('longitude')?.value;

    const latitudeProvided = latitude !== null && latitude !== '';
    const longitudeProvided = longitude !== null && longitude !== '';

    return latitudeProvided === longitudeProvided
      ? null
      : { coordinatesTogether: true };
  };

  protected readonly form = this.formBuilder.group(
    {
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
          Validators.required,
          Validators.maxLength(100),
          Validators.pattern(/^[\p{L}\s'-]+$/u),
        ],
      ],
      color: [
        '',
        [
          Validators.required,
          Validators.maxLength(100),
          Validators.pattern(/^[\p{L}\s-]+$/u),
        ],
      ],
      distinctiveMarks: ['', Validators.maxLength(500)],
      eventDate: ['', Validators.required],
      latitude: [
        null as number | null,
        [Validators.min(-90), Validators.max(90)],
      ],
      longitude: [
        null as number | null,
        [Validators.min(-180), Validators.max(180)],
      ],
      area: ['', [Validators.required, Validators.maxLength(150)]],
      contactName: [
        '',
        [
          Validators.required,
          Validators.maxLength(100),
          Validators.pattern(/^[\p{L}\s'-]+$/u),
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
    },
    { validators: this.coordinatesValidator },
  );

  protected onTypeChange(): void {
    const selectedType = this.form.controls.type.value;

    this.clearPreviews();

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
        errors.push(`${file.name}: only JPG and PNG images are allowed.`);
        continue;
      }

      if (file.size > maxSize) {
        errors.push(`${file.name}: the maximum file size is 5 MB.`);
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
    const urls = this.previewUrls();
    const urlToRemove = urls[index];

    if (urlToRemove) {
      URL.revokeObjectURL(urlToRemove);
    }

    this.selectedFiles.update((files) =>
      files.filter((_, i) => i !== index),
    );

    this.previewUrls.update((currentUrls) =>
      currentUrls.filter((_, i) => i !== index),
    );
  }

  private clearPreviews(): void {
    this.previewUrls().forEach((url) => URL.revokeObjectURL(url));
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

    const values = this.form.getRawValue();

    const announcement: AnnouncementRequest = {
      type: values.type!,
      species: values.species as Species,
      petName: (values.petName ?? '').trim(),
      distinctiveMarks: (values.distinctiveMarks ?? '').trim(),
      breed: values.breed!.trim(),
      color: values.color!.trim(),
      eventDate: values.eventDate!,
      latitude:
        values.latitude === null || values.latitude === undefined
          ? null
          : Number(values.latitude),
      longitude:
        values.longitude === null || values.longitude === undefined
          ? null
          : Number(values.longitude),
      area: values.area!.trim(),
      contactName: values.contactName!.trim(),
      contactPhone: values.contactPhone!.trim(),
    };

    this.announcementService.create(announcement).subscribe({
      next: (createdAnnouncement) => {
        const files = this.selectedFiles();

        if (files.length === 0) {
          this.submitting.set(false);
          this.successMessage.set(
            'Your announcement was created successfully!',
          );
          this.redirectToAnnouncements();
          return;
        }

        this.announcementService
          .uploadPhotos(createdAnnouncement.id, files)
          .subscribe({
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
                `Your announcement was created, but the photos could not be uploaded. Announcement ID: ${createdAnnouncement.id}.`,
              );
            },
          });
      },
      error: () => {
        this.submitting.set(false);
        this.errorMessage.set(
          'Could not create the announcement. Please check your information and try again.',
        );
      },
    });
  }
}
