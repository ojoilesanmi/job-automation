"use client";

import { useAuth } from "@/lib/auth-context";
import { Badge } from "@/components/ui/badge";

export function Header() {
  const { user } = useAuth();

  return (
    <header className="flex h-14 items-center justify-between border-b bg-card px-6">
      <div />
      <div className="flex items-center gap-2">
        {user?.roles.map((role) => (
          <Badge key={role} variant={role === "SUPER_ADMIN" ? "destructive" : role === "ADMIN" ? "warning" : "secondary"}>
            {role}
          </Badge>
        ))}
      </div>
    </header>
  );
}
