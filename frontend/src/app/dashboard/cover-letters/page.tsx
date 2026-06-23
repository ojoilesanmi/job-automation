"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { FileText, CheckCircle } from "lucide-react";

interface CoverLetter {
  id: string;
  content: string;
  jobId: string;
  jobTitle?: string;
  company?: string;
  createdAt: string;
}

interface Match {
  id: string;
  jobId: string;
  userId: string;
  matchScore: number;
  skillMatchScore: number;
  status: string;
  job: { id: string; title: string; company: string } | null;
  coverLetter: string | null;
}

interface MatchesResponse {
  matches: Match[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export default function CoverLettersPage() {
  const [letters, setLetters] = useState<CoverLetter[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<MatchesResponse>("/api/v1/matches")
      .then((data) => {
        const extracted: CoverLetter[] = data.matches
          .filter((m) => m.coverLetter)
          .map((m) => ({
            id: m.id,
            content: m.coverLetter!,
            jobId: m.jobId,
            jobTitle: m.job?.title,
            company: m.job?.company,
            createdAt: "",
          }));
        setLetters(extracted);
      })
      .catch(() => setLetters([]))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Cover Letters</h1>

      {letters.length === 0 ? (
        <Card><CardContent className="py-12 text-center text-muted-foreground">No cover letters yet. Run a match to generate one.</CardContent></Card>
      ) : (
        <div className="space-y-4">
          {letters.map((letter) => (
            <Card key={letter.id}>
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-3">
                    <FileText className="mt-1 h-5 w-5 text-muted-foreground" />
                    <div>
                      <h3 className="font-semibold">{letter.jobTitle || "Cover letter"}</h3>
                      {letter.company && (
                        <p className="text-xs text-muted-foreground">{letter.company}</p>
                      )}
                    </div>
                  </div>
                </div>
                <div className="mt-4 whitespace-pre-wrap rounded-md bg-muted/50 p-4 text-sm">{letter.content}</div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
