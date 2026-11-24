
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Conversation } from '../../models/message.model';
import { MessageService } from '../../services/message.service';

@Component({
  selector: 'app-messages',
  imports: [DatePipe, RouterLink],
  templateUrl: './messages.html',
  styleUrl: './messages.css',
})
export class Messages {
  private readonly messageService = inject(MessageService);

  protected readonly conversations = signal<Conversation[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  constructor() {
    this.loadConversations();
  }

  private loadConversations(): void {
    this.loading.set(true);
    this.error.set('');

    this.messageService.getConversations().subscribe({
      next: (conversations) => {
        this.conversations.set(conversations);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load your conversations. Please try again.');
        this.loading.set(false);
      },
    });
  }
}
