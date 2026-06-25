"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Plug } from "lucide-react";

export default function ConnectorsPage() {
  const [connectors, setConnectors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<Record<string, string>>("/api/v1/admin/discovery/connectors")
      .then(setConnectors)
      .catch(() => setConnectors({}))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  const entries = Object.entries(connectors);

  return (
    <div className="space-y-6">
      <h1 className="flex items-center gap-2 text-2xl font-bold"><Plug className="h-6 w-6" />Discovery Connectors</h1>

      {entries.length === 0 ? (
        <Card><CardContent className="py-12 text-center text-muted-foreground">No connectors available.</CardContent></Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {entries.map(([name, description]) => (
            <Card key={name}>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <Plug className="h-4 w-4" />
                  {name}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">{description}</p>
                <Badge variant="secondary" className="mt-2 text-xs">Available</Badge>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
