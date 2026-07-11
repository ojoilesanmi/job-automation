"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Brain, TrendingUp, ThumbsUp, ThumbsDown } from "lucide-react";

interface Insights {
  feedbackCounts: Record<string, number>;
  totalDecisions: number;
  approvalRate: number;
}

export default function LearningPage() {
  const [insights, setInsights] = useState<Insights | null>(null);
  const [loading, setLoading] = useState(true);
  const [autoApplying, setAutoApplying] = useState(false);
  const [autoResult, setAutoResult] = useState<{ processed: number; skipped: number } | null>(null);

  useEffect(() => {
    api.get<Insights>("/api/v1/learning/insights").then(setInsights).finally(() => setLoading(false));
  }, []);

  const triggerAutoApply = async () => {
    setAutoApplying(true);
    try {
      const data = await api.post<{ processed: number; skipped: number }>("/api/v1/learning/auto-apply");
      setAutoResult(data);
    } catch {}
    setAutoApplying(false);
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Learning & Automation</h1>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm text-muted-foreground">Total decisions</CardTitle>
            <Brain className="h-5 w-5 text-blue-600" />
          </CardHeader>
          <CardContent><p className="text-3xl font-bold">{insights?.totalDecisions ?? 0}</p></CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm text-muted-foreground">Approval rate</CardTitle>
            <TrendingUp className="h-5 w-5 text-green-600" />
          </CardHeader>
          <CardContent><p className="text-3xl font-bold">{insights?.approvalRate ?? 0}%</p></CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm text-muted-foreground">Feedback breakdown</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {Object.entries(insights?.feedbackCounts || {}).map(([type, count]) => (
              <div key={type} className="flex items-center justify-between">
                <span className="flex items-center gap-2 text-sm">
                  {type === "approved" ? <ThumbsUp className="h-3 w-3" /> : <ThumbsDown className="h-3 w-3" />}
                  {type}
                </span>
                <span className="font-medium">{count}</span>
              </div>
            ))}
            {Object.keys(insights?.feedbackCounts || {}).length === 0 && (
              <p className="text-sm text-muted-foreground">No feedback yet</p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader><CardTitle>Auto-Apply</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Auto-apply processes scored matches that meet your configured thresholds. Applications are prepared with cover letters and set to approved status.
          </p>
          <Button onClick={triggerAutoApply} disabled={autoApplying}>
            {autoApplying ? "Processing..." : "Run auto-apply now"}
          </Button>
          {autoResult && (
            <div className="rounded-lg bg-muted p-3 text-sm">
              <p>{autoResult.processed} applications auto-approved</p>
              <p>{autoResult.skipped} matches skipped</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
