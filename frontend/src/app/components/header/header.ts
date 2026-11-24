
import { Component, effect, inject, signal } from '@angular/core';
import {
  IsActiveMatchOptions,
  Router,
  RouterLink,
  RouterLinkActive,
} from '@angular/router';
import { catchError, of, switchMap, timer } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { MessageService } from '../../services/message.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  protected readonly auth = inject(AuthService);

  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);

  protected readonly unreadCount = signal(0);

  protected readonly listLinkOptions: IsActiveMatchOptions = {
    paths: 'exact',
    queryParams: 'ignored',
    fragment: 'ignored',
    matrixParams: 'ignored',
  };

  constructor() {
    effect((onCleanup) => {
      if (!this.auth.isLoggedIn()) {
        this.unreadCount.set(0);
        return;
      }

      const subscription = timer(0, 30000)
        .pipe(
          switchMap(() =>
            this.messageService.getUnreadCount().pipe(
              catchError(() => of({ count: 0 })),
            ),
          ),
        )
        .subscribe((response) => {
          this.unreadCount.set(response.count);
        });

      onCleanup(() => subscription.unsubscribe());
    });
  }

  protected logout(): void {
    this.auth.logout();
    this.router.navigate(['/announcements']);
  }
}
