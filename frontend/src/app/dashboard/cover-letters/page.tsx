"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import type { CoverLetterResponse, CoverLetterListResponse } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { FileText, Clock, Edit3, Download, Trash2, ArrowRight } from "lucide-react";

export default function CoverLettersPage() {
  const [letters, setLetters] = useState<CoverLetterResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [showComparison, setShowComparison] = useState(false);
  const [downloading, setDownloading] = useState<{ id: string; format: string } | null>(null);

  useEffect(() => {
    api
      .get<CoverLetterListResponse>("/api/v1/cover-letters")
      .then((data) => setLetters(data.coverLetters))
      .catch(() => setLetters([]))
      .finally(() => setLoading(false));
  }, []);

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      if (prev.includes(id)) return prev.filter((i) => i !== id);
      if (prev.length >= 2) return [prev[1], id];
      return [...prev, id];
    });
  };

  const selectedLetters = letters.filter((l) => selectedIds.includes(l.id));

  const handleDownload = async (id: string, format: string) => {
    setDownloading({ id, format });
    try {
      const BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
      const token = localStorage.getItem("token");
      const res = await fetch(`${BASE}/api/v1/cover-letters/${id}/export?format=${format}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!res.ok) throw new Error("Download failed");
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `cover-letter-${id}.${format}`;
      document.body.appendChild(a);
      a.click();
      URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch {}
    setDownloading(null);
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Cover Letters</h1>
        {selectedIds.length === 2 && (
          <Button onClick={() => setShowComparison(!showComparison)}>
            <ArrowRight className="mr-2 h-4 w-4" />
            {showComparison ? "Hide comparison" : "Compare selected"}
          </Button>
        )}
      </div>

      {showComparison && selectedLetters.length === 2 && (
        <Card>
          <CardHeader>
            <CardTitle>Side-by-Side Comparison</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid gap-6 md:grid-cols-2">
              {selectedLetters.map((letter) => (
                <div key={letter.id} className="space-y-3">
                  <div className="flex items-center gap-2">
                    <Badge variant="secondary">{letter.jobTitle}</Badge>
                    <Badge variant="outline">{letter.company}</Badge>
                  </div>
                  <div className="whitespace-pre-wrap rounded-md bg-muted/50 p-4 text-sm max-h-96 overflow-y-auto">
                    {letter.content}
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {letters.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            No cover letters yet. Approve a match to generate one.
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {letters.map((letter) => (
            <Card key={letter.id} className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-3">
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(letter.id)}
                      onChange={() => toggleSelect(letter.id)}
                      className="mt-1 h-4 w-4"
                    />
                    <FileText className="mt-1 h-5 w-5 text-muted-foreground" />
                    <div>
                      <h3 className="font-semibold">{letter.jobTitle}</h3>
                      <p className="text-sm text-muted-foreground">
                        {letter.company}
                      </p>
                      <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Clock className="h-3 w-3" />
                          {new Date(letter.createdAt).toLocaleDateString()}
                        </span>
                        <span>v{letter.version}</span>
                        <Badge
                          variant={
                            letter.status === "final" ? "default" : "secondary"
                          }
                          className="text-xs"
                        >
                          {letter.status}
                        </Badge>
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-1">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => handleDownload(letter.id, "pdf")}
                      disabled={downloading?.id === letter.id && downloading.format === "pdf"}
                    >
                      <Download className="h-4 w-4" />
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => handleDownload(letter.id, "docx")}
                      disabled={downloading?.id === letter.id && downloading.format === "docx"}
                    >
                      <Download className="h-4 w-4" />
                    </Button>
                    <Link href={`/dashboard/jobs/${letter.jobId}`}>
                      <Button size="sm" variant="ghost">
                        View job
                      </Button>
                    </Link>
                  </div>
                </div>
                <div className="mt-4 whitespace-pre-wrap rounded-md bg-muted/50 p-4 text-sm max-h-48 overflow-y-auto">
                  {letter.content}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
