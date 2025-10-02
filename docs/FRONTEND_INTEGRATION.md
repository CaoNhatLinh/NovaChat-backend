# 🚀 Frontend Integration Guide - Chat App Notification System

## 📦 **Required Dependencies**

### **React/TypeScript Project:**
```json
{
  "dependencies": {
    "@stomp/stompjs": "^7.0.0",
    "sockjs-client": "^1.6.1",
    "axios": "^1.6.0",
    "react-hot-toast": "^2.4.1"
  },
  "devDependencies": {
    "@types/sockjs-client": "^1.5.4"
  }
}
```

---

## 🔧 **Core Services Implementation**

### **1. WebSocket Service (websocketService.ts)**
```typescript
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface NotificationData {
  notificationId: string;
  userId: string;
  title: string;
  body: string;
  type: 'MESSAGE' | 'MENTION' | 'REACTION' | 'FRIEND_REQUEST' | 'CONVERSATION_INVITE' | 'SYSTEM' | 'POLL' | 'PIN_MESSAGE';
  metadata?: Record<string, any>;
  isRead: boolean;
  createdAt: string;
}

export interface ConversationUpdate {
  conversationId: string;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
  senderName?: string;
}

export class WebSocketService {
  private client: Client | null = null;
  private isConnected = false;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;

  // Event callbacks
  public onNotificationReceived?: (notification: NotificationData) => void;
  public onConversationUpdate?: (update: ConversationUpdate) => void;
  public onNotificationRead?: (notificationId: string) => void;
  public onAllNotificationsRead?: () => void;
  public onConnected?: () => void;
  public onDisconnected?: () => void;
  public onError?: (error: any) => void;

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        const socket = new SockJS(`${import.meta.env.VITE_API_URL}/ws`);
        
        this.client = new Client({
          webSocketFactory: () => socket,
          connectHeaders: {
            Authorization: `Bearer ${token}`
          },
          debug: (str) => {
            console.log('STOMP Debug:', str);
          },
          onConnect: () => {
            this.isConnected = true;
            this.reconnectAttempts = 0;
            this.subscribeToChannels();
            this.onConnected?.();
            resolve();
          },
          onDisconnect: () => {
            this.isConnected = false;
            this.onDisconnected?.();
          },
          onStompError: (frame) => {
            console.error('STOMP Error:', frame);
            this.onError?.(frame);
            reject(new Error(frame.headers['message']));
          },
          onWebSocketClose: (event) => {
            this.isConnected = false;
            this.attemptReconnect(token);
          }
        });

        this.client.activate();
      } catch (error) {
        reject(error);
      }
    });
  }

  private subscribeToChannels() {
    if (!this.client) return;

    // Subscribe to personal notifications
    this.client.subscribe('/user/queue/notifications', (message: IMessage) => {
      try {
        const notification: NotificationData = JSON.parse(message.body);
        this.onNotificationReceived?.(notification);
      } catch (error) {
        console.error('Error parsing notification:', error);
      }
    });

    // Subscribe to conversation updates
    this.client.subscribe('/user/queue/conversation-updates', (message: IMessage) => {
      try {
        const update: ConversationUpdate = JSON.parse(message.body);
        this.onConversationUpdate?.(update);
      } catch (error) {
        console.error('Error parsing conversation update:', error);
      }
    });

    // Subscribe to notification read events
    this.client.subscribe('/user/queue/notification-read', (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        this.onNotificationRead?.(data.notificationId);
      } catch (error) {
        console.error('Error parsing notification read:', error);
      }
    });

    // Subscribe to all notifications read events
    this.client.subscribe('/user/queue/notifications-read-all', (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        if (data.success) {
          this.onAllNotificationsRead?.();
        }
      } catch (error) {
        console.error('Error parsing notifications read all:', error);
      }
    });
  }

  markNotificationAsRead(notificationId: string) {
    if (this.client && this.isConnected) {
      this.client.publish({
        destination: '/app/notification.read',
        body: JSON.stringify({ notificationId })
      });
    }
  }

  markAllNotificationsAsRead() {
    if (this.client && this.isConnected) {
      this.client.publish({
        destination: '/app/notifications.read-all',
        body: '{}'
      });
    }
  }

  private attemptReconnect(token: string) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      
      setTimeout(() => {
        this.connect(token).catch(() => {
          // Will retry on next attempt
        });
      }, this.reconnectDelay * this.reconnectAttempts);
    }
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.isConnected = false;
    }
  }

  isClientConnected(): boolean {
    return this.isConnected && this.client?.connected === true;
  }
}

// Singleton instance
export const webSocketService = new WebSocketService();
```

### **2. Notification API Service (notificationApi.ts)**
```typescript
import axios from 'axios';
import { NotificationData } from './websocketService';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

// Add auth interceptor
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export interface NotificationPage {
  content: NotificationData[];
  hasNext: boolean;
  hasContent: boolean;
}

export interface UnreadCountResponse {
  count: number;
}

export class NotificationApiService {
  async getNotifications(page = 0, size = 20): Promise<NotificationPage> {
    const response = await api.get(`/api/notifications?page=${page}&size=${size}`);
    return response.data;
  }

  async getUnreadNotifications(): Promise<NotificationData[]> {
    const response = await api.get('/api/notifications/unread');
    return response.data;
  }

  async getUnreadCount(): Promise<number> {
    const response = await api.get<UnreadCountResponse>('/api/notifications/unread/count');
    return response.data.count;
  }

  async getNotificationsByType(type: string, page = 0, size = 10): Promise<NotificationPage> {
    const response = await api.get(`/api/notifications/type/${type}?page=${page}&size=${size}`);
    return response.data;
  }

  async markAsRead(notificationId: string): Promise<void> {
    await api.put(`/api/notifications/${notificationId}/read`);
  }

  async markAllAsRead(): Promise<void> {
    await api.put('/api/notifications/read-all');
  }

  async createTestNotification(data: {
    title: string;
    body: string;
    type: string;
    metadata?: Record<string, any>;
  }): Promise<NotificationData> {
    const response = await api.post('/api/notifications/test', data);
    return response.data;
  }
}

export const notificationApi = new NotificationApiService();
```

### **3. Notification Store (Zustand) (notificationStore.ts)**
```typescript
import { create } from 'zustand';
import { subscribeWithSelector } from 'zustand/middleware';
import { NotificationData, ConversationUpdate } from './websocketService';
import { notificationApi } from './notificationApi';
import toast from 'react-hot-toast';

interface NotificationStore {
  // State
  notifications: NotificationData[];
  unreadCount: number;
  conversations: Record<string, ConversationUpdate>;
  isLoading: boolean;
  hasMore: boolean;
  currentPage: number;

  // Actions
  addNotification: (notification: NotificationData) => void;
  updateConversation: (update: ConversationUpdate) => void;
  markNotificationAsRead: (notificationId: string) => void;
  markAllNotificationsAsRead: () => void;
  loadNotifications: (page?: number) => Promise<void>;
  loadUnreadCount: () => Promise<void>;
  clearNotifications: () => void;
  
  // Computed
  getUnreadNotifications: () => NotificationData[];
  getNotificationsByType: (type: string) => NotificationData[];
}

export const useNotificationStore = create<NotificationStore>()(
  subscribeWithSelector((set, get) => ({
    // Initial state
    notifications: [],
    unreadCount: 0,
    conversations: {},
    isLoading: false,
    hasMore: true,
    currentPage: 0,

    // Actions
    addNotification: (notification) => {
      set((state) => ({
        notifications: [notification, ...state.notifications],
        unreadCount: notification.isRead ? state.unreadCount : state.unreadCount + 1
      }));

      // Show toast notification
      if (!notification.isRead) {
        toast.success(notification.title, {
          duration: 4000,
          position: 'top-right',
        });
      }
    },

    updateConversation: (update) => {
      set((state) => ({
        conversations: {
          ...state.conversations,
          [update.conversationId]: update
        }
      }));
    },

    markNotificationAsRead: (notificationId) => {
      set((state) => {
        const notification = state.notifications.find(n => n.notificationId === notificationId);
        if (!notification || notification.isRead) return state;

        return {
          notifications: state.notifications.map(n =>
            n.notificationId === notificationId ? { ...n, isRead: true } : n
          ),
          unreadCount: Math.max(0, state.unreadCount - 1)
        };
      });
    },

    markAllNotificationsAsRead: () => {
      set((state) => ({
        notifications: state.notifications.map(n => ({ ...n, isRead: true })),
        unreadCount: 0
      }));
    },

    loadNotifications: async (page = 0) => {
      if (get().isLoading) return;

      set({ isLoading: true });
      try {
        const result = await notificationApi.getNotifications(page, 20);
        
        set((state) => ({
          notifications: page === 0 ? result.content : [...state.notifications, ...result.content],
          hasMore: result.hasNext,
          currentPage: page,
          isLoading: false
        }));
      } catch (error) {
        console.error('Failed to load notifications:', error);
        set({ isLoading: false });
        toast.error('Failed to load notifications');
      }
    },

    loadUnreadCount: async () => {
      try {
        const count = await notificationApi.getUnreadCount();
        set({ unreadCount: count });
      } catch (error) {
        console.error('Failed to load unread count:', error);
      }
    },

    clearNotifications: () => {
      set({
        notifications: [],
        unreadCount: 0,
        conversations: {},
        hasMore: true,
        currentPage: 0
      });
    },

    // Computed
    getUnreadNotifications: () => {
      return get().notifications.filter(n => !n.isRead);
    },

    getNotificationsByType: (type) => {
      return get().notifications.filter(n => n.type === type);
    }
  }))
);

// Subscribe to changes and sync with localStorage
useNotificationStore.subscribe(
  (state) => state.unreadCount,
  (unreadCount) => {
    // Update document title
    if (unreadCount > 0) {
      document.title = `(${unreadCount}) Chat App`;
    } else {
      document.title = 'Chat App';
    }
  }
);
```

---

## 🎨 **React Components**

### **4. Notification Component (NotificationItem.tsx)**
```typescript
import React from 'react';
import { formatDistanceToNow } from 'date-fns';
import { vi } from 'date-fns/locale';
import { NotificationData } from '../services/websocketService';
import { useNotificationStore } from '../store/notificationStore';
import { webSocketService } from '../services/websocketService';

interface NotificationItemProps {
  notification: NotificationData;
  onClick?: () => void;
}

const getNotificationIcon = (type: string) => {
  switch (type) {
    case 'MESSAGE': return '💬';
    case 'MENTION': return '👋';
    case 'REACTION': return '❤️';
    case 'FRIEND_REQUEST': return '👥';
    case 'CONVERSATION_INVITE': return '🎉';
    case 'POLL': return '📊';
    case 'PIN_MESSAGE': return '📌';
    case 'SYSTEM': return '⚙️';
    default: return '🔔';
  }
};

export const NotificationItem: React.FC<NotificationItemProps> = ({ notification, onClick }) => {
  const markNotificationAsRead = useNotificationStore(state => state.markNotificationAsRead);

  const handleClick = () => {
    if (!notification.isRead) {
      markNotificationAsRead(notification.notificationId);
      webSocketService.markNotificationAsRead(notification.notificationId);
    }
    onClick?.();
  };

  const timeAgo = formatDistanceToNow(new Date(notification.createdAt), {
    addSuffix: true,
    locale: vi
  });

  return (
    <div
      className={`notification-item ${!notification.isRead ? 'unread' : ''}`}
      onClick={handleClick}
    >
      <div className="notification-icon">
        {getNotificationIcon(notification.type)}
      </div>
      
      <div className="notification-content">
        <h4 className="notification-title">{notification.title}</h4>
        <p className="notification-body">{notification.body}</p>
        <div className="notification-meta">
          <span className="notification-type">{notification.type}</span>
          <span className="notification-time">{timeAgo}</span>
        </div>
      </div>

      {!notification.isRead && (
        <div className="unread-indicator" />
      )}
    </div>
  );
};
```

### **5. Notification List Component (NotificationList.tsx)**
```typescript
import React, { useEffect, useCallback } from 'react';
import { NotificationItem } from './NotificationItem';
import { useNotificationStore } from '../store/notificationStore';
import { useInfiniteScroll } from '../hooks/useInfiniteScroll';

export const NotificationList: React.FC = () => {
  const {
    notifications,
    unreadCount,
    isLoading,
    hasMore,
    currentPage,
    loadNotifications,
    markAllNotificationsAsRead
  } = useNotificationStore();

  const loadMore = useCallback(() => {
    if (!isLoading && hasMore) {
      loadNotifications(currentPage + 1);
    }
  }, [isLoading, hasMore, currentPage, loadNotifications]);

  const [sentinelRef] = useInfiniteScroll(loadMore);

  useEffect(() => {
    loadNotifications(0);
  }, []);

  const handleMarkAllAsRead = () => {
    markAllNotificationsAsRead();
    webSocketService.markAllNotificationsAsRead();
  };

  return (
    <div className="notification-list">
      <div className="notification-header">
        <h2>
          Thông báo 
          {unreadCount > 0 && (
            <span className="unread-badge">{unreadCount}</span>
          )}
        </h2>
        
        {unreadCount > 0 && (
          <button 
            className="mark-all-read-btn"
            onClick={handleMarkAllAsRead}
          >
            Đánh dấu tất cả đã đọc
          </button>
        )}
      </div>

      <div className="notification-items">
        {notifications.length === 0 && !isLoading ? (
          <div className="empty-state">
            <p>Không có thông báo nào</p>
          </div>
        ) : (
          notifications.map((notification) => (
            <NotificationItem
              key={notification.notificationId}
              notification={notification}
              onClick={() => {
                // Handle notification click (navigate to conversation, etc.)
                if (notification.metadata?.conversationId) {
                  // Navigate to conversation
                  window.location.href = `/chat/${notification.metadata.conversationId}`;
                }
              }}
            />
          ))
        )}

        {hasMore && (
          <div ref={sentinelRef} className="loading-sentinel">
            {isLoading && <div className="loading-spinner">Đang tải...</div>}
          </div>
        )}
      </div>
    </div>
  );
};
```

### **6. Notification Hook (useNotifications.ts)**
```typescript
import { useEffect } from 'react';
import { useNotificationStore } from '../store/notificationStore';
import { webSocketService } from '../services/websocketService';
import { useAuth } from './useAuth';

export const useNotifications = () => {
  const { user, token } = useAuth();
  const { addNotification, updateConversation, markNotificationAsRead, markAllNotificationsAsRead, loadUnreadCount } = useNotificationStore();

  useEffect(() => {
    if (!user || !token) return;

    // Setup WebSocket event handlers
    webSocketService.onNotificationReceived = (notification) => {
      addNotification(notification);
    };

    webSocketService.onConversationUpdate = (update) => {
      updateConversation(update);
    };

    webSocketService.onNotificationRead = (notificationId) => {
      markNotificationAsRead(notificationId);
    };

    webSocketService.onAllNotificationsRead = () => {
      markAllNotificationsAsRead();
    };

    webSocketService.onConnected = () => {
      console.log('WebSocket connected for notifications');
      loadUnreadCount();
    };

    webSocketService.onError = (error) => {
      console.error('WebSocket error:', error);
    };

    // Connect WebSocket
    webSocketService.connect(token).catch(console.error);

    // Load initial unread count
    loadUnreadCount();

    // Cleanup on unmount
    return () => {
      webSocketService.disconnect();
    };
  }, [user, token]);

  return {
    sendTestNotification: () => {
      // For testing purposes
      addNotification({
        notificationId: crypto.randomUUID(),
        userId: user?.id || '',
        title: 'Test Notification',
        body: 'This is a test notification',
        type: 'SYSTEM',
        isRead: false,
        createdAt: new Date().toISOString()
      });
    }
  };
};
```

---

## 🎨 **CSS Styles**

### **7. Notification Styles (notifications.css)**
```css
.notification-list {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e7eb;
}

.notification-header h2 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.unread-badge {
  background: #ef4444;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.mark-all-read-btn {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.mark-all-read-btn:hover {
  background: #2563eb;
}

.notification-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.notification-item:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.notification-item.unread {
  background: #fef3f2;
  border-color: #fecaca;
}

.notification-icon {
  font-size: 24px;
  margin-top: 2px;
}

.notification-content {
  flex: 1;
}

.notification-title {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.notification-body {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.4;
}

.notification-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #9ca3af;
}

.notification-type {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  text-transform: uppercase;
  font-weight: 500;
}

.unread-indicator {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.loading-sentinel {
  display: flex;
  justify-content: center;
  padding: 20px;
}

.loading-spinner {
  color: #6b7280;
  font-size: 14px;
}

/* Responsive */
@media (max-width: 768px) {
  .notification-list {
    padding: 16px;
  }
  
  .notification-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .notification-item {
    padding: 12px;
  }
}
```

Với implementation này, bạn sẽ có một hệ thống notification hoàn chỉnh với:

✅ **Real-time notifications qua WebSocket**
✅ **Infinite scroll loading**
✅ **Toast notifications** 
✅ **Unread count tracking**
✅ **Conversation updates**
✅ **Mark as read functionality**
✅ **Responsive design**
✅ **Error handling và reconnection**

Hệ thống này sẽ tự động cập nhật danh sách conversation khi có tin nhắn mới và hiển thị notifications real-time! 🎉
