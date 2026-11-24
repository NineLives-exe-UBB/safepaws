
export interface MessageRequest {
  body: string;
  recipientId?: number;
}

export interface MessageResponse {
  id: number;
  senderId: number;
  recipientId: number;
  body: string;
  createdAt: string;
  readAt: string | null;
}

export interface ConversationUser {
  id: number;
  displayName: string;
}

export interface ConversationLastMessage {
  body: string;
  createdAt: string;
  fromMe: boolean;
}

export interface Conversation {
  announcementId: number;
  petName: string | null;
  species: 'CAT' | 'DOG' | 'OTHER';
  type: 'LOST' | 'FOUND';
  otherUser: ConversationUser;
  lastMessage: ConversationLastMessage;
  unreadCount: number;
}

export interface UnreadCountResponse {
  count: number;
}
