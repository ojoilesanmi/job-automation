"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Plus, Shield } from "lucide-react";

interface Role {
  id: string;
  name: string;
  description: string;
}

export default function RolesPage() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);
  const [newRole, setNewRole] = useState({ name: "", description: "" });
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    api.get<Role[]>("/api/v1/admin/roles").then(setRoles).catch(() => setRoles([])).finally(() => setLoading(false));
  }, []);

  const create = async () => {
    if (!newRole.name) return;
    setCreating(true);
    try {
      const role = await api.post<Role>("/api/v1/admin/roles", newRole);
      setRoles([...roles, role]);
      setNewRole({ name: "", description: "" });
    } catch {}
    setCreating(false);
  };

  const deleteRole = async (id: string) => {
    try {
      await api.del(`/api/v1/admin/roles/${id}`);
      setRoles(roles.filter((r) => r.id !== id));
    } catch {}
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="flex items-center gap-2 text-2xl font-bold"><Shield className="h-6 w-6" />Roles</h1>

      <Card>
        <CardHeader><CardTitle>Create role</CardTitle></CardHeader>
        <CardContent className="flex gap-3">
          <Input placeholder="Role name" value={newRole.name} onChange={(e) => setNewRole({ ...newRole, name: e.target.value })} />
          <Input placeholder="Description" value={newRole.description} onChange={(e) => setNewRole({ ...newRole, description: e.target.value })} />
          <Button onClick={create} disabled={creating}>
            <Plus className="mr-2 h-4 w-4" />{creating ? "Creating..." : "Create"}
          </Button>
        </CardContent>
      </Card>

      <div className="space-y-3">
        {roles.map((role) => (
          <Card key={role.id}>
            <CardContent className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3">
                <Badge variant="outline" className="text-base">{role.name}</Badge>
                <span className="text-sm text-muted-foreground">{role.description}</span>
              </div>
              <Button size="sm" variant="destructive" onClick={() => deleteRole(role.id)}>Delete</Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
