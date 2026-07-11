"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Link2, ArrowLeft } from "lucide-react";

export default function JobImportPage() {
  const router = useRouter();
  const [url, setUrl] = useState("");
  const [title, setTitle] = useState("");
  const [company, setCompany] = useState("");
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState("");

  const handleImport = async () => {
    if (!url.trim()) { setError("URL is required"); return; }
    setImporting(true);
    setError("");
    try {
      const result = await api.post<{ id: string }>("/api/v1/jobs/import-url", { url, title: title || undefined, company: company || undefined });
      router.push(`/dashboard/jobs/${result.id}`);
    } catch (e: any) {
      setError(e.message || "Import failed");
    }
    setImporting(false);
  };

  return (
    <div className="space-y-6 max-w-xl">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => router.back()}><ArrowLeft className="h-5 w-5" /></Button>
        <h1 className="text-2xl font-bold">Import Job from URL</h1>
      </div>

      <Card>
        <CardHeader><CardTitle>Job URL</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-medium">URL *</label>
            <div className="relative">
              <Link2 className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input className="pl-9" placeholder="https://..." value={url} onChange={(e) => setUrl(e.target.value)} />
            </div>
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium">Title (optional)</label>
            <Input placeholder="Job title" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium">Company (optional)</label>
            <Input placeholder="Company name" value={company} onChange={(e) => setCompany(e.target.value)} />
          </div>
          {error && <p className="text-sm text-red-500">{error}</p>}
          <Button className="w-full" onClick={handleImport} disabled={importing}>
            {importing ? "Importing..." : "Import Job"}
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
