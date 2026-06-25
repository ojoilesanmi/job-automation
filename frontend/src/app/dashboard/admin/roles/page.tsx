"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Plus, Shield, Trash2, ChevronDown, ChevronUp } from "lucide-react";

interface Permission {
  id: string;
  name: string;
  description: string;
}

interface Role {
  id: string;
  name: string;
  description: string;
  permissions: string[];
  createdAt: string;
}

export default function RolesPage() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [allPermissions, setAllPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(true);
  const [newRole, setNewRole] = useState({ name: "", description: "" });
  const [creating, setCreating] = useState(false);
  const [expandedRole, setExpandedRole] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      api.get<Role[]>("/api/v1/admin/roles"),
      api.get<Permission[]>("/api/v1/admin/permissions"),
    ]).then(([r, p]) => { setRoles(r); setAllPermissions(p); })
      .catch(() => {})
      .finally(() => setLoading(false));
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

  const togglePermission = async (roleId: string, permName: string, hasIt: boolean) => {
    const role = roles.find((r) => r.id === roleId);
    if (!role) return;
    const newPerms = hasIt
      ? role.permissions.filter((p) => p !== permName)
      : [...role.permissions, permName];
    try {
      const updated = await api.put<Role>(`/api/v1/admin/roles/${roleId}`, {
        name: role.name,
        description: role.description,
        permissions: newPerms,
      });
      setRoles(roles.map((r) => r.id === roleId ? updated : r));
    } catch {}
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="flex items-center gap-2 text-2xl font-bold"><Shield className="h-6 w-6" />Roles & Permissions</h1>

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
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Badge variant="outline" className="text-base">{role.name}</Badge>
                  <span className="text-sm text-muted-foreground">{role.description}</span>
                  <Badge variant="secondary" className="text-xs">{role.permissions.length} permissions</Badge>
                </div>
                <div className="flex gap-2">
                  <Button size="sm" variant="ghost" onClick={() => setExpandedRole(expandedRole === role.id ? null : role.id)}>
                    {expandedRole === role.id ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                  </Button>
                  {role.name !== "SUPER_ADMIN" && (
                    <Button size="sm" variant="ghost" onClick={() => deleteRole(role.id)}>
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  )}
                </div>
              </div>

              {expandedRole === role.id && (
                <div className="mt-4 border-t pt-4">
                  <p className="text-xs font-medium text-muted-foreground mb-2">Permissions</p>
                  <div className="flex flex-wrap gap-2">
                    {allPermissions.map((perm) => {
                      const has = role.permissions.includes(perm.name);
                      return (
                        <button
                          key={perm.name}
                          onClick={() => togglePermission(role.id, perm.name, has)}
                          className={`rounded-md border px-2.5 py-1 text-xs transition-colors ${
                            has
                              ? "border-primary bg-primary/10 text-primary"
                              : "border-muted-foreground/25 text-muted-foreground hover:border-primary/50"
                          }`}
                          title={perm.description}
                        >
                          {perm.name}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
