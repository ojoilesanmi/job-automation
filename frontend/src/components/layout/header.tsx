"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth-context";
import { api } from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Bell } from "lucide-react";

export function Header() {
  const { user } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState<{ id: string; title: string; message: string; read: boolean }[]>([]);
  const [showNotifs, setShowNotifs] = useState(false);

  useEffect(() => {
    api.get<{ count: number }>("/api/v1/notifications/unread-count")
      .then((d) => setUnreadCount(d.count))
      .catch(() => {});
  }, []);

  const toggleNotifs = async () => {
    if (!showNotifs) {
      try {
        const data = await api.get<{ notifications: { id: string; title: string; message: string; read: boolean }[] }>("/api/v1/notifications?size=10");
        setNotifications(data.notifications || []);
      } catch {}
    }
    setShowNotifs(!showNotifs);
  };

  const markAllRead = async () => {
    try {
      await api.post("/api/v1/notifications/read-all");
      setUnreadCount(0);
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    } catch {}
  };

  return (
    <header className="flex h-14 items-center justify-between border-b bg-card px-6">
      <div />
      <div className="flex items-center gap-3">
        <div className="relative">
          <Button variant="ghost" size="icon" onClick={toggleNotifs}>
            <Bell className="h-5 w-5" />
            {unreadCount > 0 && (
              <span className="absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] text-white">
                {unreadCount > 9 ? "9+" : unreadCount}
              </span>
            )}
          </Button>
          {showNotifs && (
            <div className="absolute right-0 top-full mt-2 w-80 rounded-lg border bg-card shadow-lg z-50">
              <div className="flex items-center justify-between border-b p-3">
                <span className="font-medium text-sm">Notifications</span>
                {unreadCount > 0 && (
                  <Button variant="ghost" size="sm" onClick={markAllRead}>Mark all read</Button>
                )}
              </div>
              <div className="max-h-64 overflow-y-auto">
                {notifications.length === 0 ? (
                  <p className="p-4 text-sm text-muted-foreground text-center">No notifications</p>
                ) : (
                  notifications.map((n) => (
                    <div key={n.id} className={`border-b p-3 last:border-0 ${!n.read ? "bg-accent/50" : ""}`}>
                      <p className="text-sm font-medium">{n.title}</p>
                      <p className="text-xs text-muted-foreground">{n.message}</p>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>
        {user?.roles.map((role) => (
          <Badge key={role} variant={role === "SUPER_ADMIN" ? "destructive" : role === "ADMIN" ? "warning" : "secondary"}>
            {role}
          </Badge>
        ))}
      </div>
    </header>
  );
}
