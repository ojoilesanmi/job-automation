"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { FileText, Clock, Edit3 } from "lucide-react";

interface CoverLetter {
  id: string;
  jobId: string;
  jobTitle: string;
  company: string;
  content: string;
  version: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

interface CoverLetterListResponse {
  coverLetters: CoverLetter[];
}

export default function CoverLettersPage() {
  const [letters, setLetters] = useState<CoverLetter[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .get<CoverLetterListResponse>("/api/v1/cover-letters")
      .then((data) => setLetters(data.coverLetters))
      .catch(() => setLetters([]))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Cover Letters</h1>

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
                  <Link href={`/dashboard/jobs/${letter.jobId}`}>
                    <Button size="sm" variant="ghost">
                      View job
                    </Button>
                  </Link>
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
