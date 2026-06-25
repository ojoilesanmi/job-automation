"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/auth-context";
import {
  LayoutDashboard,
  User,
  Briefcase,
  FileText,
  Mail,
  Settings,
  Shield,
  Users,
  Database,
  LogOut,
  Target,
  Zap,
  ScrollText,
  Plug,
} from "lucide-react";

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  permission?: string;
}

const navItems: NavItem[] = [
  { label: "Overview", href: "/dashboard/overview", icon: LayoutDashboard },
  { label: "My Profile", href: "/dashboard/profile", icon: User },
  { label: "Preferences", href: "/dashboard/preferences", icon: Settings },
  { label: "Jobs", href: "/dashboard/jobs", icon: Briefcase },
  { label: "Matches", href: "/dashboard/matches", icon: Zap },
  { label: "Applications", href: "/dashboard/applications", icon: Target },
  { label: "Cover Letters", href: "/dashboard/cover-letters", icon: FileText },
];

const adminItems: NavItem[] = [
  { label: "Users", href: "/dashboard/admin/users", icon: Users, permission: "user.read" },
  { label: "Roles", href: "/dashboard/admin/roles", icon: Shield, permission: "role.read" },
  { label: "Job Sources", href: "/dashboard/admin/job-sources", icon: Database, permission: "job-source.read" },
  { label: "Connectors", href: "/dashboard/admin/connectors", icon: Plug, permission: "job-source.read" },
  { label: "Audit Logs", href: "/dashboard/admin/audit-logs", icon: ScrollText, permission: "admin:audit:read" },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user, logout, hasPermission } = useAuth();

  return (
    <aside className="flex h-screen w-64 flex-col border-r bg-card">
      <div className="flex h-14 items-center border-b px-4">
        <Mail className="mr-2 h-6 w-6 text-primary" />
        <span className="text-lg font-bold">JobAgent</span>
      </div>

      <nav className="flex-1 space-y-1 p-3">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground",
              pathname === item.href ? "bg-accent text-accent-foreground" : "text-muted-foreground"
            )}
          >
            <item.icon className="h-4 w-4" />
            {item.label}
          </Link>
        ))}

        {adminItems.some((i) => !i.permission || hasPermission(i.permission)) && (
          <>
            <div className="my-2 border-t" />
            <p className="px-3 py-1 text-xs font-semibold uppercase text-muted-foreground">Admin</p>
            {adminItems
              .filter((i) => !i.permission || hasPermission(i.permission))
              .map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground",
                    pathname === item.href ? "bg-accent text-accent-foreground" : "text-muted-foreground"
                  )}
                >
                  <item.icon className="h-4 w-4" />
                  {item.label}
                </Link>
              ))}
          </>
        )}
      </nav>

      <div className="border-t p-3">
        <div className="mb-2 px-3 text-sm">
          <p className="font-medium">{user?.firstName} {user?.lastName}</p>
          <p className="text-xs text-muted-foreground">{user?.email}</p>
        </div>
        <button
          onClick={logout}
          className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
        >
          <LogOut className="h-4 w-4" />
          Sign out
        </button>
      </div>
    </aside>
  );
}
