"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";

export function useProtectedRoute(requiredPermission?: string) {
  const { user, loading } = useAuth();
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (loading) return;
    if (!user) { router.push("/auth/login"); return; }
    if (requiredPermission && !user.permissions.includes(requiredPermission)) {
      router.push("/dashboard/overview");
      return;
    }
    setReady(true);
  }, [user, loading, router, requiredPermission]);

  return { user, loading, ready };
}
