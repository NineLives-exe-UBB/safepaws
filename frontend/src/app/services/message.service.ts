
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Conversation,
  MessageRequest,
  MessageResponse,
  UnreadCountResponse,
} from '../models/message.model';

@Injectable({
  providedIn: 'root',
})
export class MessageService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  sendMessage(
    announcementId: number,
    request: MessageRequest,
  ): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${this.apiUrl}/announcements/${announcementId}/messages`,
      request,
    );
  }

  getConversation(
    announcementId: number,
    withUserId?: number,
  ): Observable<MessageResponse[]> {
    let params = new HttpParams();

    if (withUserId !== undefined) {
      params = params.set('with', withUserId);
    }

    return this.http.get<MessageResponse[]>(
      `${this.apiUrl}/announcements/${announcementId}/messages`,
      { params },
    );
  }

  getConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(`${this.apiUrl}/conversations`);
  }

  getUnreadCount(): Observable<UnreadCountResponse> {
    return this.http.get<UnreadCountResponse>(
      `${this.apiUrl}/messages/unread-count`,
    );
  }
}
