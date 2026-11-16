
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

const passwordsMatchValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmation = control.get('confirmPassword')?.value;

  if (!password || !confirmation) {
    return null;
  }

  return password === confirmation ? null : { passwordsMismatch: true };
};

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email]],
      displayName: ['', [Validators.required, Validators.maxLength(100)]],
      password: [
        '',
        [Validators.required, Validators.pattern(PASSWORD_PATTERN)],
      ],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatchValidator },
  );

  protected get passwordsMismatch(): boolean {
    return (
      this.form.hasError('passwordsMismatch') &&
      this.form.controls.confirmPassword.touched
    );
  }

  protected get passwordRules(): {
    label: string;
    valid: boolean;
  }[] {
    const password = this.form.controls.password.value;

    return [
      {
        label: 'At least 8 characters.',
        valid: password.length >= 8,
      },
      {
        label: 'One uppercase letter.',
        valid: /[A-Z]/.test(password),
      },
      {
        label: 'One digit.',
        valid: /\d/.test(password),
      },
      {
        label: 'One special character.',
        valid: /[^A-Za-z0-9]/.test(password),
      },
    ];
  }

  protected onSubmit(): void {
    this.form.markAllAsTouched();
    this.errorMessage.set('');

    if (this.form.invalid || this.submitting()) {
      return;
    }

    this.submitting.set(true);

    const values = this.form.getRawValue();

    this.authService.register({
      email: values.email.trim(),
      displayName: values.displayName.trim(),
      password: values.password,
    }).subscribe({
      next: () => {
        this.submitting.set(false);

        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(this.safeReturnUrl(returnUrl));
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.showBackendErrors(error);
      },
    });
  }

  private showBackendErrors(error: unknown): void {
    if (error instanceof HttpErrorResponse) {
      const fieldErrors: { field: string; message: string }[] =
        error.error?.fieldErrors ?? [];

      if (fieldErrors.length > 0) {
        for (const fieldError of fieldErrors) {
          const field = fieldError.field === 'display_name'
            ? 'displayName'
            : fieldError.field;

          const control = this.form.get(field);

          if (control) {
            control.setErrors({
              ...control.errors,
              server: `${fieldError.message.replace(/[.!?]*$/, '')}.`,
            });
            control.markAsTouched();
          }
        }

        return;
      }

      if (error.status === 409) {
        this.form.controls.email.setErrors({
          ...this.form.controls.email.errors,
          server: 'This email address is already registered.',
        });
        this.form.controls.email.markAsTouched();
        return;
      }
    }

    this.errorMessage.set(
      'Could not create your account. Please try again.',
    );
  }

  protected serverError(field: string): string | null {
    return this.form.get(field)?.getError('server') ?? null;
  }

  private safeReturnUrl(url: string | null): string {
    if (
      url &&
      url.startsWith('/') &&
      !url.startsWith('//') &&
      !url.startsWith('/login') &&
      !url.startsWith('/register')
    ) {
      return url;
    }

    return '/announcements';
  }
}
