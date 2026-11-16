
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected onSubmit(): void {
    this.form.markAllAsTouched();
    this.errorMessage.set('');

    if (this.form.invalid || this.submitting()) {
      return;
    }

    this.submitting.set(true);

    const credentials = this.form.getRawValue();

    this.authService.login({
      email: credentials.email.trim(),
      password: credentials.password,
    }).subscribe({
      next: () => {
        this.submitting.set(false);

        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(this.safeReturnUrl(returnUrl));
      },
      error: (error: unknown) => {
        this.submitting.set(false);

        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.errorMessage.set('Invalid email or password.');
          return;
        }

        this.errorMessage.set(
          'Could not log in. Please try again.',
        );
      },
    });
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
