"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Users } from "lucide-react";

interface UserRow {
  id: string;
  email: string;
  fullName: string;
  roles: string[];
  createdAt: string;
}

interface UsersResponse {
  users: UserRow[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export default function UsersPage() {
  const [users, setUsers] = useState<UserRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    api.get<UsersResponse>("/api/v1/admin/users")
      .then((data) => { setUsers(data.users); setTotal(data.totalElements); })
      .catch(() => setUsers([]))
      .finally(() => setLoading(false));
  }, []);

  const toggleRole = async (userId: string, role: string, add: boolean) => {
    try {
      if (add) {
        const updated = await api.post<UserRow>(`/api/v1/admin/users/${userId}/roles`, { roleName: role });
        setUsers(users.map((u) => u.id === userId ? { ...u, roles: updated.roles } : u));
      } else {
        const updated = await api.del<UserRow>(`/api/v1/admin/users/${userId}/roles/${role}`);
        setUsers(users.map((u) => u.id === userId ? { ...u, roles: updated.roles } : u));
      }
    } catch {}
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-2xl font-bold"><Users className="h-6 w-6" />Users</h1>
        <p className="text-sm text-muted-foreground">{total} users</p>
      </div>

      <Card>
        <CardContent className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-muted-foreground">
                <th className="p-3">Name</th>
                <th className="p-3">Email</th>
                <th className="p-3">Roles</th>
                <th className="p-3">Joined</th>
                <th className="p-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id} className="border-b">
                  <td className="p-3 font-medium">{user.fullName}</td>
                  <td className="p-3 text-muted-foreground">{user.email}</td>
                  <td className="p-3">
                    <div className="flex gap-1">
                      {user.roles.map((r) => <Badge key={r} variant="secondary">{r}</Badge>)}
                    </div>
                  </td>
                  <td className="p-3 text-muted-foreground text-xs">
                    {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : "—"}
                  </td>
                  <td className="p-3">
                    <div className="flex gap-1">
                      {!user.roles.includes("ADMIN") && (
                        <Button size="sm" variant="outline" onClick={() => toggleRole(user.id, "ADMIN", true)}>Make admin</Button>
                      )}
                      {user.roles.includes("ADMIN") && !user.roles.includes("SUPER_ADMIN") && (
                        <Button size="sm" variant="ghost" onClick={() => toggleRole(user.id, "ADMIN", false)}>Remove admin</Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  );
}
