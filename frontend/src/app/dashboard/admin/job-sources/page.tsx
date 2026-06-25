"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Plus, Database, Power, PowerOff, Trash2, RefreshCw } from "lucide-react";

interface JobSource {
  id: string;
  name: string;
  sourceType: string;
  baseUrl: string;
  enabled: boolean;
  configJson: Record<string, unknown>;
  createdAt: string;
}

export default function JobSourcesPage() {
  const [sources, setSources] = useState<JobSource[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [newSource, setNewSource] = useState({ name: "", sourceType: "api", baseUrl: "" });
  const [discovering, setDiscovering] = useState<string | null>(null);

  useEffect(() => {
    api.get<JobSource[]>("/api/v1/admin/job-sources").then(setSources).catch(() => setSources([])).finally(() => setLoading(false));
  }, []);

  const create = async () => {
    if (!newSource.name) return;
    setCreating(true);
    try {
      const src = await api.post<JobSource>("/api/v1/admin/job-sources", newSource);
      setSources([...sources, src]);
      setNewSource({ name: "", sourceType: "api", baseUrl: "" });
    } catch {}
    setCreating(false);
  };

  const toggleSource = async (id: string) => {
    try {
      await api.patch(`/api/v1/admin/job-sources/${id}/toggle`);
      setSources(sources.map((s) => (s.id === id ? { ...s, enabled: !s.enabled } : s)));
    } catch {}
  };

  const deleteSource = async (id: string) => {
    try {
      await api.del(`/api/v1/admin/job-sources/${id}`);
      setSources(sources.filter((s) => s.id !== id));
    } catch {}
  };

  const runDiscovery = async (id: string) => {
    setDiscovering(id);
    try {
      await api.post(`/api/v1/admin/job-sources/${id}/discover`);
    } catch {}
    setDiscovering(null);
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="flex items-center gap-2 text-2xl font-bold"><Database className="h-6 w-6" />Job Sources</h1>

      <Card>
        <CardHeader><CardTitle>Add job source</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-3 gap-4">
            <Input placeholder="Name" value={newSource.name} onChange={(e) => setNewSource({ ...newSource, name: e.target.value })} />
            <Input placeholder="Type (api/scrape)" value={newSource.sourceType} onChange={(e) => setNewSource({ ...newSource, sourceType: e.target.value })} />
            <Input placeholder="Base URL" value={newSource.baseUrl} onChange={(e) => setNewSource({ ...newSource, baseUrl: e.target.value })} />
          </div>
          <Button onClick={create} disabled={creating}>
            <Plus className="mr-2 h-4 w-4" />{creating ? "Adding..." : "Add source"}
          </Button>
        </CardContent>
      </Card>

      <div className="space-y-3">
        {sources.map((src) => (
          <Card key={src.id}>
            <CardContent className="flex items-center justify-between p-4">
              <div>
                <div className="flex items-center gap-3">
                  <h3 className="font-medium">{src.name}</h3>
                  <Badge variant="outline">{src.sourceType}</Badge>
                  <Badge variant={src.enabled ? "default" : "secondary"}>
                    {src.enabled ? "Active" : "Disabled"}
                  </Badge>
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  {src.baseUrl || "No URL"} &middot; Created: {src.createdAt ? new Date(src.createdAt).toLocaleDateString() : "—"}
                </p>
              </div>
              <div className="flex gap-2">
                <Button size="sm" variant="outline" onClick={() => runDiscovery(src.id)} disabled={discovering === src.id}>
                  <RefreshCw className={`mr-1 h-3 w-3 ${discovering === src.id ? "animate-spin" : ""}`} />
                  {discovering === src.id ? "Running..." : "Discover"}
                </Button>
                <Button size="sm" variant="outline" onClick={() => toggleSource(src.id)}>
                  {src.enabled ? <><PowerOff className="mr-1 h-3 w-3" />Disable</> : <><Power className="mr-1 h-3 w-3" />Enable</>}
                </Button>
                <Button size="sm" variant="ghost" onClick={() => deleteSource(src.id)}>
                  <Trash2 className="h-3 w-3" />
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
