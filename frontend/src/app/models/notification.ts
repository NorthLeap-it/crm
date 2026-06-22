// entities/Notification.java
export interface Notification {
  id: string;
  userId: string;
  title: string;
  body: string | null;
  link: string | null;
  readAt: string | null;
  createdAt: string;
}
