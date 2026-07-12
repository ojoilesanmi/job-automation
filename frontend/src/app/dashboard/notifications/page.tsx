"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Bell,
  CheckCircle,
  AlertCircle,
  Info,
  Clock,
  Trash2,
  CheckCheck,
} from "lucide-react";

interface Notification {
  id: string;
  type: string;
  title: string;
  message: string;
  referenceId: string | null;
  referenceType: string | null;
  read: boolean;
  createdAt: string;
}

interface NotificationsResponse {
  notifications: Notification[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

const typeConfig: Record<
  string,
  {
    label: string;
    variant: string;
    icon: React.ComponentType<{ className?: string }>;
  }
> = {
  strong_match: {
    label: "Strong Match",
    variant: "success",
    icon: CheckCircle,
  },
  pending_approval: {
    label: "Pending Approval",
    variant: "warning",
    icon: Clock,
  },
  application_submitted: {
    label: "Submitted",
    variant: "default",
    icon: CheckCircle,
  },
  follow_up_due: {
    label: "Follow-up Due",
    variant: "warning",
    icon: AlertCircle,
  },
  interview_invite: {
    label: "Interview Invite",
    variant: "success",
    icon: CheckCircle,
  },
  rejection: {
    label: "Rejection",
    variant: "destructive",
    icon: AlertCircle,
  },
  assessment: {
    label: "Assessment",
    variant: "default",
    icon: Info,
  },
  weekly_summary: {
    label: "Weekly Summary",
    variant: "secondary",
    icon: Info,
  },
};

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    Promise.all([
      api.get<NotificationsResponse>("/api/v1/notifications"),
      api.get<{ count: number }>("/api/v1/notifications/unread-count"),
    ])
      .then(([notifData, countData]) => {
        setNotifications(notifData.notifications);
        setUnreadCount(countData.count);
      })
      .catch(() => {
        setNotifications([]);
        setUnreadCount(0);
      })
      .finally(() => setLoading(false));
  }, []);

  const markAsRead = async (id: string) => {
    try {
      await api.post(`/api/v1/notifications/${id}/read`, {});
      setNotifications(
        notifications.map((n) => (n.id === id ? { ...n, read: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {}
  };

  const markAllAsRead = async () => {
    try {
      await api.post("/api/v1/notifications/read-all", {});
      setNotifications(notifications.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch {}
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold">Notifications</h1>
          {unreadCount > 0 && (
            <Badge variant="destructive">{unreadCount} unread</Badge>
          )}
        </div>
        {unreadCount > 0 && (
          <Button variant="outline" size="sm" onClick={markAllAsRead}>
            <CheckCheck className="mr-2 h-4 w-4" />
            Mark all as read
          </Button>
        )}
      </div>

      {notifications.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            <Bell className="mx-auto mb-4 h-12 w-12 opacity-50" />
            No notifications yet.
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {notifications.map((notif) => {
            const cfg = typeConfig[notif.type] || {
              label: notif.type,
              variant: "secondary",
              icon: Bell,
            };
            const Icon = cfg.icon;

            return (
              <Card
                key={notif.id}
                className={`transition-colors ${
                  !notif.read
                    ? "border-l-4 border-l-primary bg-muted/50"
                    : "opacity-70"
                }`}
              >
                <CardContent className="flex items-start justify-between p-4">
                  <div className="flex items-start gap-3">
                    <Icon className="mt-0.5 h-5 w-5 text-muted-foreground" />
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{notif.title}</span>
                        <Badge variant={cfg.variant as any}>
                          {cfg.label}
                        </Badge>
                      </div>
                      <p className="mt-1 text-sm text-muted-foreground">
                        {notif.message}
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {new Date(notif.createdAt).toLocaleString()}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {!notif.read && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => markAsRead(notif.id)}
                      >
                        <CheckCircle className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
