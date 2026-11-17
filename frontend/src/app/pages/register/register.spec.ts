import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Register } from './register';

describe('Register', () => {
  let fixture: ComponentFixture<Register>;
  let element: HTMLElement;
  let router: Router;
  let register: ReturnType<typeof vi.fn>;

  async function render(returnUrl: string | null = null): Promise<void> {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(returnUrl ? { returnUrl } : {}),
            },
          },
        },
        {
          provide: AuthService,
          useValue: { register },
        },
      ],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture = TestBed.createComponent(Register);
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  function fill(selector: string, value: string): void {
    const input = element.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }

  function fillValidForm(): void {
    fill('#email', 'owner@example.com');
    fill('#displayName', 'Test Owner');
    fill('#password', 'Password1!');
    fill('#confirmPassword', 'Password1!');
  }

  async function submit(): Promise<void> {
    element.querySelector('form')!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
    await fixture.whenStable();
  }

  beforeEach(() => {
    register = vi.fn(() => of({
      id: 5,
      email: 'owner@example.com',
      displayName: 'Test Owner',
    }));
  });

  it('should require all fields', async () => {
    await render();
    await submit();

    expect(element.textContent).toContain('Email is required.');
    expect(element.textContent).toContain('Display name is required.');
    expect(element.textContent).toContain('Password is required.');
    expect(element.textContent).toContain('Please confirm your password.');
    expect(register).not.toHaveBeenCalled();
  });

  it('should reject an invalid email', async () => {
    await render();
    fillValidForm();
    fill('#email', 'invalid-email');
    await submit();

    expect(element.textContent).toContain('Enter a valid email address.');
    expect(register).not.toHaveBeenCalled();
  });

  it('should enforce password requirements', async () => {
    await render();
    fillValidForm();
    fill('#password', 'password');
    fill('#confirmPassword', 'password');
    await submit();

    expect(element.textContent).toContain(
      'Your password does not meet all the requirements.',
    );
    expect(register).not.toHaveBeenCalled();
  });

  it('should reject passwords that do not match', async () => {
    await render();
    fillValidForm();
    fill('#confirmPassword', 'Different1!');
    await submit();

    expect(element.textContent).toContain('Passwords do not match.');
    expect(register).not.toHaveBeenCalled();
  });

  it('should reject display names longer than 50 characters', async () => {
    await render();
    fillValidForm();
    fill('#displayName', 'A'.repeat(51));
    await submit();

    expect(element.textContent).toContain(
      'Display name cannot exceed 50 characters.',
    );
    expect(register).not.toHaveBeenCalled();
  });

  it('should register and return to the requested page', async () => {
    await render('/announcements/new');
    fillValidForm();
    fill('#email', ' owner@example.com ');
    fill('#displayName', ' Test Owner ');
    await submit();

    expect(register).toHaveBeenCalledWith({
      email: 'owner@example.com',
      displayName: 'Test Owner',
      password: 'Password1!',
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/announcements/new');
  });

  it('should show duplicate-email errors under the email field', async () => {
    register.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409 })),
    );

    await render();
    fillValidForm();
    await submit();

    expect(element.textContent).toContain(
      'This email address is already registered.',
    );
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('should display backend validation errors under the correct field', async () => {
    register.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 400,
        error: {
          fieldErrors: [
            { field: 'display_name', message: 'Display name is invalid' },
          ],
        },
      })),
    );

    await render();
    fillValidForm();
    await submit();

    expect(element.textContent).toContain('Display name is invalid.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('should show a general error when registration fails', async () => {
    register.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    await render();
    fillValidForm();
    await submit();

    expect(element.textContent).toContain(
      'Could not create your account. Please try again.',
    );
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('should reject an external return URL', async () => {
    await render('//example.com');
    fillValidForm();
    await submit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/announcements');
  });

  it('should clear the general error when the form is sent again', async () => {
    register
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })))
      .mockReturnValueOnce(of({ id: 5, email: 'owner@example.com', displayName: 'Test Owner' }));

    await render();
    fillValidForm();
    await submit();
    expect(element.textContent).toContain('Could not create your account. Please try again.');

    await submit();

    expect(element.textContent).not.toContain('Could not create your account.');
    expect(element.querySelector('[role="alert"]')).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/announcements');
  });

  it('should disable the button while saving and send one request', async () => {
    const response = new Subject<{ id: number; email: string; displayName: string }>();
    register.mockReturnValue(response);

    await render();
    fillValidForm();
    await submit();
    await submit();

    const button = element.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    expect(register).toHaveBeenCalledTimes(1);
    expect(button.disabled).toBe(true);

    response.next({ id: 5, email: 'owner@example.com', displayName: 'Test Owner' });
    response.complete();
    await fixture.whenStable();

    expect(button.disabled).toBe(false);
    expect(router.navigateByUrl).toHaveBeenCalledTimes(1);
  });

  it('should tick the password rules as the password is typed', async () => {
    await render();

    const rules = async (password: string): Promise<string[]> => {
      fill('#password', password);
      await fixture.whenStable();
      return Array.from(element.querySelectorAll('.password-rules small')).map(
        (rule) => `${rule.classList.contains('rule-valid') ? 'ok' : 'no'} ${rule.textContent?.trim()}`,
      );
    };

    expect(await rules('')).toEqual([
      'no • At least 8 characters.',
      'no • One uppercase letter.',
      'no • One digit.',
      'no • One special character.',
    ]);
    expect(await rules('safepaws')).toEqual([
      'ok ✓ At least 8 characters.',
      'no • One uppercase letter.',
      'no • One digit.',
      'no • One special character.',
    ]);
    expect(await rules('Safepaws')).toEqual([
      'ok ✓ At least 8 characters.',
      'ok ✓ One uppercase letter.',
      'no • One digit.',
      'no • One special character.',
    ]);
    expect(await rules('Safepaws1')).toEqual([
      'ok ✓ At least 8 characters.',
      'ok ✓ One uppercase letter.',
      'ok ✓ One digit.',
      'no • One special character.',
    ]);
    expect(await rules('Safepaws1!')).toEqual([
      'ok ✓ At least 8 characters.',
      'ok ✓ One uppercase letter.',
      'ok ✓ One digit.',
      'ok ✓ One special character.',
    ]);
    expect(await rules('Sp1!')).toEqual([
      'no • At least 8 characters.',
      'ok ✓ One uppercase letter.',
      'ok ✓ One digit.',
      'ok ✓ One special character.',
    ]);
  });
});
