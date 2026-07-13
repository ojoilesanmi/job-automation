"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import type { DetectedQuestion } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ArrowLeft, HelpCircle, CheckCircle, FileText, List, Upload } from "lucide-react";

export default function JobQuestionsPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [questions, setQuestions] = useState<DetectedQuestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [detecting, setDetecting] = useState(false);

  const detectQuestions = async () => {
    setDetecting(true);
    try {
      const data = await api.post<{ questions: DetectedQuestion[] }>("/api/v1/ats/detect-questions", { jobId: id });
      setQuestions(data.questions);
    } catch {}
    setDetecting(false);
  };

  useEffect(() => {
    detectQuestions().finally(() => setLoading(false));
  }, [id]);

  const getTypeIcon = (type: string) => {
    switch (type) {
      case "file": return <Upload className="h-3 w-3" />;
      case "select": return <List className="h-3 w-3" />;
      default: return <FileText className="h-3 w-3" />;
    }
  };

  const answeredCount = questions.filter((q) => q.answered).length;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => router.back()}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <h1 className="text-2xl font-bold">Application Questions</h1>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm text-muted-foreground">Total questions</CardTitle>
            <HelpCircle className="h-5 w-5 text-blue-600" />
          </CardHeader>
          <CardContent><p className="text-3xl font-bold">{questions.length}</p></CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm text-muted-foreground">Answered</CardTitle>
            <CheckCircle className="h-5 w-5 text-green-600" />
          </CardHeader>
          <CardContent><p className="text-3xl font-bold">{answeredCount}</p></CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm text-muted-foreground">Pending</CardTitle>
            <HelpCircle className="h-5 w-5 text-yellow-600" />
          </CardHeader>
          <CardContent><p className="text-3xl font-bold">{questions.length - answeredCount}</p></CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Detected Questions</CardTitle>
            <Button variant="outline" onClick={detectQuestions} disabled={detecting}>
              {detecting ? "Detecting..." : "Re-detect"}
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          {questions.map((question) => (
            <div
              key={question.id}
              className="flex items-start gap-3 rounded-md border p-4"
            >
              {question.answered ? (
                <CheckCircle className="mt-0.5 h-5 w-5 shrink-0 text-green-600" />
              ) : (
                <HelpCircle className="mt-0.5 h-5 w-5 shrink-0 text-muted-foreground" />
              )}
              <div className="flex-1">
                <p className="text-sm font-medium">{question.text}</p>
                <div className="mt-2 flex items-center gap-2">
                  <Badge variant="secondary" className="flex items-center gap-1 text-xs">
                    {getTypeIcon(question.type)}
                    {question.type}
                  </Badge>
                  {question.required && (
                    <Badge variant="destructive" className="text-xs">Required</Badge>
                  )}
                  {question.answered && (
                    <Badge variant="default" className="text-xs">Answered</Badge>
                  )}
                </div>
              </div>
            </div>
          ))}
          {questions.length === 0 && !detecting && (
            <p className="py-8 text-center text-muted-foreground">
              No questions detected for this job posting.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
