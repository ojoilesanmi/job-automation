"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Plus, Database, Power, PowerOff } from "lucide-react";

interface JobSource {
  id: string;
  name: string;
  type: string;
  url: string;
  enabled: boolean;
  lastRunAt: string;
  jobsFound: number;
}

export default function JobSourcesPage() {
  const [sources, setSources] = useState<JobSource[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [newSource, setNewSource] = useState({ name: "", type: "api", url: "" });

  useEffect(() => {
    api.get<JobSource[]>("/api/v1/admin/job-sources").then(setSources).catch(() => setSources([])).finally(() => setLoading(false));
  }, []);

  const create = async () => {
    if (!newSource.name) return;
    setCreating(true);
    try {
      const src = await api.post<JobSource>("/api/v1/admin/job-sources", newSource);
      setSources([...sources, src]);
      setNewSource({ name: "", type: "api", url: "" });
    } catch {}
    setCreating(false);
  };

  const toggleSource = async (id: string, enabled: boolean) => {
    try {
      await api.put(`/api/v1/admin/job-sources/${id}`, { enabled: !enabled });
      setSources(sources.map((s) => (s.id === id ? { ...s, enabled: !enabled } : s)));
    } catch {}
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
            <Input placeholder="Type (api/scrape)" value={newSource.type} onChange={(e) => setNewSource({ ...newSource, type: e.target.value })} />
            <Input placeholder="URL" value={newSource.url} onChange={(e) => setNewSource({ ...newSource, url: e.target.value })} />
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
                  <Badge variant="outline">{src.type}</Badge>
                  <Badge variant={src.enabled ? "success" : "secondary"}>
                    {src.enabled ? "Active" : "Disabled"}
                  </Badge>
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  {src.url} &middot; {src.jobsFound} jobs &middot; Last run: {src.lastRunAt ? new Date(src.lastRunAt).toLocaleString() : "Never"}
                </p>
              </div>
              <Button size="sm" variant="outline" onClick={() => toggleSource(src.id, src.enabled)}>
                {src.enabled ? <><PowerOff className="mr-2 h-3 w-3" />Disable</> : <><Power className="mr-2 h-3 w-3" />Enable</>}
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
