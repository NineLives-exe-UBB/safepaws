import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let element: HTMLElement;
  let router: Router;
  let login: ReturnType<typeof vi.fn>;

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
          useValue: { login },
        },
      ],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture = TestBed.createComponent(Login);
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  function fill(selector: string, value: string): void {
    const input = element.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }

  async function submit(): Promise<void> {
    element.querySelector('form')!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
    await fixture.whenStable();
  }

  beforeEach(() => {
    login = vi.fn(() => of({
      id: 5,
      email: 'owner@example.com',
      displayName: 'Test Owner',
    }));
  });

  it('should require email and password', async () => {
    await render();
    await submit();

    expect(element.textContent).toContain('Email is required.');
    expect(element.textContent).toContain('Password is required.');
    expect(login).not.toHaveBeenCalled();
  });

  it('should reject an invalid email', async () => {
    await render();

    fill('#email', 'invalid-email');
    fill('#password', 'Password1!');
    await submit();

    expect(element.textContent).toContain('Enter a valid email address.');
    expect(login).not.toHaveBeenCalled();
  });

  it('should show an error for incorrect credentials', async () => {
    login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401 })),
    );

    await render();

    fill('#email', 'owner@example.com');
    fill('#password', 'WrongPassword1!');
    await submit();

    expect(element.textContent).toContain('Invalid email or password.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('should return to the requested page after login', async () => {
    await render('/announcements/9');

    fill('#email', ' owner@example.com ');
    fill('#password', 'Password1!');
    await submit();

    expect(login).toHaveBeenCalledWith({
      email: 'owner@example.com',
      password: 'Password1!',
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/announcements/9');
  });

  it('should use the announcement list as the default destination', async () => {
    await render();

    fill('#email', 'owner@example.com');
    fill('#password', 'Password1!');
    await submit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/announcements');
  });

  it('should reject an external return URL', async () => {
    await render('//example.com');

    fill('#email', 'owner@example.com');
    fill('#password', 'Password1!');
    await submit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/announcements');
  });
});
