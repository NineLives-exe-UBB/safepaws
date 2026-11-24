import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AnnouncementRequest, Species } from '../../models/announcement.model';
import { Coordinates, MapPin } from '../../models/map.model';
import { AnnouncementService } from '../../services/announcement.service';
import { LocationService } from '../../services/location.service';
import { MapView } from '../../components/map/map';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

/** Long enough to read the success message before the details page opens. */
const REDIRECT_DELAY_MS = 1500;

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
  private readonly route = inject(ActivatedRoute);

  /** Set on /announcements/:id/edit: the form loads that announcement and saves with PUT. */
  protected readonly editId = this.parseEditId();
  protected readonly loadingAnnouncement = signal(false);
  /** Photos the announcement already has; the backend keeps its main photo when more are added. */
  protected readonly existingPhotoCount = signal(0);

  protected readonly selectedFiles = signal<File[]>([]);
  protected readonly previewUrls = signal<string[]>([]);
  protected readonly submitting = signal(false);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');

  private readonly authService = inject(AuthService);

  protected readonly mapCenter = signal<Coordinates | null>(null);
  protected readonly selectedLocation = signal<Coordinates | null>(null);

  /** The latest date the picker offers: now, in the user's local time. */
  protected readonly maxEventDate = this.toDateTimeInputValue(new Date());

  private redirectTimeout?: ReturnType<typeof setTimeout>;
  protected createdAnnouncementId?: number;

  private readonly notInFutureValidator: ValidatorFn = (
    control: AbstractControl,
  ): ValidationErrors | null =>
    control.value && new Date(control.value).getTime() > Date.now() ? { futureDate: true } : null;

  protected readonly form = this.formBuilder.group({
    type: ['LOST' as 'LOST' | 'FOUND', Validators.required],
    species: ['' as Species | '', Validators.required],

    petName: ['', [Validators.maxLength(50)]],

    breed: ['', [Validators.maxLength(100)]],

    color: ['', [Validators.maxLength(100)]],

    distinctiveMarks: ['', Validators.maxLength(500)],

    eventDate: ['', [Validators.required, this.notInFutureValidator]],

    latitude: [
      null as number | null,
      [Validators.required, Validators.min(-90), Validators.max(90)],
    ],

    longitude: [
      null as number | null,
      [Validators.required, Validators.min(-180), Validators.max(180)],
    ],

    area: ['', [Validators.required, Validators.maxLength(150), Validators.pattern(/\S/)]],

    contactName: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)]],

    contactPhone: [
  '',
  [Validators.maxLength(20), Validators.pattern(/^(\+?[0-9]{7,15})?$/)],
],
  });

  private readonly selectedType = toSignal(this.form.controls.type.valueChanges, {
    initialValue: this.form.controls.type.value,
  });

  ngOnInit(): void {
    if (this.editId !== undefined) {
      this.loadAnnouncement(this.editId);
      return;
    }

    const displayName = this.authService.user()?.displayName;

    if (displayName && !this.form.controls.contactName.value) {
      this.form.controls.contactName.setValue(displayName);
    }

    this.locationService.getCurrentLocation().then((location) => {
      if (location) {
        this.mapCenter.set(location);
      }
    });
  }

  private parseEditId(): number | undefined {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    return Number.isInteger(id) && id > 0 ? id : undefined;
  }

  /** Fills the form with the announcement; only its owner may edit it. */
  private loadAnnouncement(id: number): void {
    this.loadingAnnouncement.set(true);
    this.form.disable();

    this.announcementService.getById(id).subscribe({
      next: (announcement) => {
        this.loadingAnnouncement.set(false);

        if (announcement.ownerId !== this.authService.user()?.id) {
          this.errorMessage.set('You can only change your own announcements.');
          return;
        }

        this.existingPhotoCount.set(announcement.photos.length);
        this.form.enable();
        this.form.patchValue({
          type: announcement.type,
          species: announcement.species,
          petName: announcement.petName ?? '',
          breed: announcement.breed ?? '',
          color: announcement.color ?? '',
          distinctiveMarks: announcement.distinctiveMarks ?? '',
          eventDate: announcement.eventDate.slice(0, 16),
          area: announcement.area,
          contactName: announcement.contactName,
          contactPhone: announcement.contactPhone,
        });

        if (announcement.latitude !== null && announcement.longitude !== null) {
          const location = { latitude: announcement.latitude, longitude: announcement.longitude };
          this.mapCenter.set(location);
          this.selectedLocation.set(location);
          this.form.controls.latitude.setValue(location.latitude);
          this.form.controls.longitude.setValue(location.longitude);
        }

      },
      error: (error: unknown) => {
        this.loadingAnnouncement.set(false);
        this.errorMessage.set(
          error instanceof HttpErrorResponse && error.status === 404
            ? 'This announcement does not exist.'
            : 'Could not load the announcement. Please try again.',
        );
      },
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
    if (this.submitting()) {
      return;
    }

    this.selectedLocation.set(location);

    this.form.controls.latitude.setValue(location.latitude);
    this.form.controls.longitude.setValue(location.longitude);

    this.form.controls.latitude.markAsTouched();
    this.form.controls.longitude.markAsTouched();

    this.form.controls.latitude.markAsDirty();
    this.form.controls.longitude.markAsDirty();
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

      const alreadySelected = [...this.selectedFiles(), ...validFiles].some(
        (existing) =>
          existing.name === file.name &&
          existing.size === file.size &&
          existing.lastModified === file.lastModified,
      );

      if (!alreadySelected) {
        validFiles.push(file);
      }
    }

    const newPreviewUrls = validFiles.map((file) => URL.createObjectURL(file));

    this.selectedFiles.update((currentFiles) => [...currentFiles, ...validFiles]);

    this.previewUrls.update((currentUrls) => [...currentUrls, ...newPreviewUrls]);

    this.errorMessage.set(errors.join(' '));
    input.value = '';
  }

  protected removeFile(index: number): void {
    const urlToRemove = this.previewUrls()[index];

    if (urlToRemove) {
      URL.revokeObjectURL(urlToRemove);
    }

    this.selectedFiles.update((files) => files.filter((_, i) => i !== index));

    this.previewUrls.update((urls) => urls.filter((_, i) => i !== index));
  }

  private clearPreviews(): void {
    this.previewUrls().forEach((url) => URL.revokeObjectURL(url));

    this.previewUrls.set([]);
  }

  private redirectToDetails(announcementId: number): void {
    if (this.redirectTimeout) {
      clearTimeout(this.redirectTimeout);
    }

    this.redirectTimeout = setTimeout(() => {
      this.router.navigate(['/announcements', announcementId]);
    }, REDIRECT_DELAY_MS);
  }

  ngOnDestroy(): void {
    this.clearPreviews();

    if (this.redirectTimeout) {
      clearTimeout(this.redirectTimeout);
    }
  }

  protected onSubmit(): void {
    this.form.markAllAsTouched();

    if (this.form.invalid || this.form.disabled || this.submitting()) {
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
      contactPhone: values.contactPhone?.trim() || undefined,
    };

    if (this.editId !== undefined) {
      this.saveChanges(this.editId, announcement);
      return;
    }

    this.announcementService.create(announcement).subscribe({
      next: (createdAnnouncement) => {
        this.createdAnnouncementId = createdAnnouncement.id;
        this.uploadSelectedPhotos(createdAnnouncement.id);
      },

      error: (error: unknown) => {
        this.submitting.set(false);
        this.errorMessage.set(
          this.showFieldErrors(error)
            ? 'Some details are not valid. Please check the highlighted fields.'
            : 'Could not create the announcement. Please check your information and try again.',
        );
      },
    });
  }

  private saveChanges(id: number, announcement: AnnouncementRequest): void {
    this.announcementService.update(id, announcement).subscribe({
      next: () => {
        // A photo retry after this only uploads the photos, like on the create page.
        this.createdAnnouncementId = id;
        this.uploadSelectedPhotos(id);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        if (error instanceof HttpErrorResponse && error.status === 403) {
          this.errorMessage.set('You can only change your own announcements.');
          return;
        }
        this.errorMessage.set(
          this.showFieldErrors(error)
            ? 'Some details are not valid. Please check the highlighted fields.'
            : 'Could not save the changes. Please check your information and try again.',
        );
      },
    });
  }

  private uploadSelectedPhotos(announcementId: number): void {
    const files = this.selectedFiles();

    if (files.length === 0) {
      this.submitting.set(false);

      this.successMessage.set(
        this.editId !== undefined
          ? 'Your changes were saved.'
          : 'Your announcement was created successfully!',
      );

      this.redirectToDetails(announcementId);
      return;
    }

    this.announcementService.uploadPhotos(announcementId, files).subscribe({
      next: () => {
        this.submitting.set(false);

        this.successMessage.set(
          this.editId !== undefined
            ? 'Your changes and photos were saved.'
            : 'Your announcement and photos were uploaded successfully!',
        );

        this.redirectToDetails(announcementId);
      },

      error: () => {
        this.submitting.set(false);

        this.errorMessage.set(
          (this.editId !== undefined ? 'Your changes were saved' : 'Your announcement was created') +
            ', but the photos could not be uploaded. Check the photos and submit again to retry, or remove them to continue without photos.',
        );
      },
    });
  }

  /** Shows the backend's validation errors next to their fields. Returns false if none matched a field. */
  private showFieldErrors(error: unknown): boolean {
    if (!(error instanceof HttpErrorResponse) || error.status !== 400) {
      return false;
    }
    const fieldErrors: { field: string; message: string }[] = error.error?.fieldErrors ?? [];
    let shown = false;
    for (const { field, message } of fieldErrors) {
      // the backend reports a missing half of the coordinates as "coordinates"
      const names = field === 'coordinates' ? ['latitude', 'longitude'] : [field];
      for (const name of names) {
        const control = this.form.get(name);
        if (control) {
          control.setErrors({ ...control.errors, server: message });
          control.markAsTouched();
          shown = true;
        }
      }
    }
    return shown;
  }

  protected serverError(name: string): string | null {
    return this.form.get(name)?.getError('server') ?? null;
  }

  /** Formats a date the way a datetime-local input expects it: YYYY-MM-DDTHH:mm, local time. */
  private toDateTimeInputValue(date: Date): string {
    const localTime = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return localTime.toISOString().slice(0, 16);
  }

  private optionalText(value: string | null | undefined): string | undefined {
    return value?.trim() || undefined;
  }
}
