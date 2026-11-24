
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, of, switchMap, timer } from 'rxjs';

import { MessageResponse } from '../../models/message.model';
import { AnnouncementService } from '../../services/announcement.service';
import { AuthService } from '../../services/auth.service';
import { MessageService } from '../../services/message.service';

@Component({
  selector: 'app-conversation',
  imports: [DatePipe, RouterLink],
  templateUrl: './conversation.html',
  styleUrl: './conversation.css',
})
export class ConversationPage {
  private readonly route = inject(ActivatedRoute);
  private readonly messageService = inject(MessageService);
  private readonly announcementService = inject(AnnouncementService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly announcementId = Number(
    this.route.snapshot.paramMap.get('announcementId'),
  );

  protected readonly withUserId = Number(
    this.route.snapshot.paramMap.get('withUserId'),
  );

  protected readonly currentUserId = computed(() => this.auth.user()?.id ?? null);

  protected readonly announcement = signal<{
    petName: string | null;
    ownerId: number | null;
  } | null>(null);

  protected readonly messages = signal<MessageResponse[]>([]);
  protected readonly draft = signal('');
  protected readonly loading = signal(true);
  protected readonly sending = signal(false);
  protected readonly error = signal('');
  protected readonly sendError = signal('');

  protected readonly isOwner = computed(() => {
    const ownerId = this.announcement()?.ownerId;
    const userId = this.currentUserId();

    return ownerId !== null && ownerId !== undefined && ownerId === userId;
  });

  constructor() {
    if (
      !Number.isInteger(this.announcementId) ||
      this.announcementId <= 0 ||
      !Number.isInteger(this.withUserId) ||
      this.withUserId <= 0
    ) {
      this.error.set('Invalid conversation.');
      this.loading.set(false);
      return;
    }

    this.loadAnnouncement();
  }

  private loadAnnouncement(): void {
    this.announcementService
      .getById(this.announcementId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (announcement) => {
          this.announcement.set({
            petName: announcement.petName,
            ownerId: announcement.ownerId,
          });

          // An owner must select another participant, not themselves.
          if (this.isOwner() && this.withUserId === announcement.ownerId) {
            this.error.set('Invalid conversation recipient.');
            this.loading.set(false);
            return;
          }

          this.startMessageRefresh();
        },
        error: () => {
          this.error.set('Could not load the announcement.');
          this.loading.set(false);
        },
      });
  }

  /**
   * Non-owners omit the "with" parameter: the backend defaults to the owner.
   * Owners provide the ID of the person they are replying to.
   */
  private getConversationMessages() {
    return this.messageService.getConversation(
      this.announcementId,
      this.isOwner() ? this.withUserId : undefined,
    );
  }

  private startMessageRefresh(): void {
    timer(0, 30000)
      .pipe(
        switchMap(() =>
          this.getConversationMessages().pipe(
            catchError(() => {
              this.error.set('Could not load messages. Please try again.');
              this.loading.set(false);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((messages) => {
        if (messages !== null) {
          this.messages.set(messages);
          this.error.set('');
          this.loading.set(false);
        }
      });
  }

  protected updateDraft(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    this.draft.set(textarea.value);
  }

  protected sendMessage(): void {
    const body = this.draft().trim();

    if (!body || this.sending() || body.length > 1000) {
      return;
    }

    const finalBody = body.endsWith('.') ? body : `${body}.`;

    if (finalBody.length > 1000) {
      this.sendError.set(
        'Your message must be at most 1000 characters, including the final full stop.',
      );
      return;
    }

    this.sending.set(true);
    this.sendError.set('');

    this.messageService
      .sendMessage(this.announcementId, {
        body: finalBody,
        ...(this.isOwner() ? { recipientId: this.withUserId } : {}),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.draft.set('');
          this.sending.set(false);
          this.refreshMessages();
        },
        error: () => {
          this.sendError.set('Could not send your message. Please try again.');
          this.sending.set(false);
        },
      });
  }

  private refreshMessages(): void {
    this.getConversationMessages()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (messages) => {
          this.messages.set(messages);
          this.error.set('');
        },
        error: () => {
          this.error.set('Could not refresh the conversation.');
        },
      });
  }
}
