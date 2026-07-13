"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { SkillGap, ExperienceGap, GapAnalysis } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { AlertTriangle, BookOpen, Briefcase, TrendingUp, Search } from "lucide-react";

export default function LearningGapsPage() {
  const [analysis, setAnalysis] = useState<GapAnalysis | null>(null);
  const [loading, setLoading] = useState(false);
  const [initialLoad, setInitialLoad] = useState(true);
  const [jobId, setJobId] = useState("");

  const runAnalysis = async (id?: string) => {
    setLoading(true);
    try {
      const data = id
        ? await api.post<GapAnalysis>("/api/v1/learning/gaps", { jobId: id })
        : await api.get<GapAnalysis>("/api/v1/learning/gaps");
      setAnalysis(data);
    } catch {}
    setLoading(false);
  };

  useEffect(() => {
    api
      .get<GapAnalysis>("/api/v1/learning/gaps")
      .then((data) => setAnalysis(data))
      .catch(() => {})
      .finally(() => setInitialLoad(false));
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    runAnalysis(jobId || undefined);
  };

  const criticalSkills = analysis?.missingSkills.filter((s) => s.severity === "critical") || [];
  const niceToHaveSkills = analysis?.missingSkills.filter((s) => s.severity !== "critical") || [];

  if (initialLoad) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Skill Gap Analysis</h1>

      <Card>
        <CardHeader>
          <CardTitle>Analyze Gaps for a Job</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex gap-3">
            <div className="flex-1 space-y-1">
              <Label htmlFor="jobId">Job ID (optional)</Label>
              <Input
                id="jobId"
                placeholder="Enter job ID to analyze against"
                value={jobId}
                onChange={(e) => setJobId(e.target.value)}
              />
            </div>
            <Button type="submit" disabled={loading} className="self-end">
              <Search className="mr-2 h-4 w-4" />
              {loading ? "Analyzing..." : "Analyze"}
            </Button>
          </form>
        </CardContent>
      </Card>

      {analysis && (
        <>
          <Card>
            <CardHeader><CardTitle>Summary</CardTitle></CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">{analysis.summary}</p>
            </CardContent>
          </Card>

          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm text-muted-foreground">Critical gaps</CardTitle>
                <AlertTriangle className="h-5 w-5 text-red-600" />
              </CardHeader>
              <CardContent><p className="text-3xl font-bold">{criticalSkills.length}</p></CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm text-muted-foreground">Nice-to-have gaps</CardTitle>
                <TrendingUp className="h-5 w-5 text-blue-600" />
              </CardHeader>
              <CardContent><p className="text-3xl font-bold">{niceToHaveSkills.length}</p></CardContent>
            </Card>
          </div>

          {criticalSkills.length > 0 && (
            <Card className="border-red-200">
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-red-700">
                  <AlertTriangle className="h-5 w-5" />
                  Critical Skill Gaps
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {criticalSkills.map((skill) => (
                  <div key={skill.skill} className="rounded-md border border-red-200 p-4">
                    <div className="flex items-center gap-2">
                      <Badge variant="destructive">{skill.skill}</Badge>
                      <Badge variant="outline">Critical</Badge>
                    </div>
                    <p className="mt-2 text-sm text-muted-foreground">{skill.recommendation}</p>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}

          {niceToHaveSkills.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <BookOpen className="h-5 w-5 text-blue-600" />
                  Nice-to-Have Skill Gaps
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {niceToHaveSkills.map((skill) => (
                  <div key={skill.skill} className="rounded-md border p-4">
                    <div className="flex items-center gap-2">
                      <Badge variant="secondary">{skill.skill}</Badge>
                      <Badge variant="outline">{skill.severity}</Badge>
                    </div>
                    <p className="mt-2 text-sm text-muted-foreground">{skill.recommendation}</p>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}

          {analysis.experienceGaps.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Briefcase className="h-5 w-5 text-yellow-600" />
                  Experience Gaps
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {analysis.experienceGaps.map((gap) => (
                  <div key={gap.area} className="rounded-md border p-4">
                    <h4 className="font-medium">{gap.area}</h4>
                    <p className="mt-1 text-sm text-muted-foreground">{gap.description}</p>
                    <p className="mt-2 text-sm font-medium text-primary">{gap.suggestion}</p>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}
        </>
      )}

      {!analysis && !loading && (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            Run an analysis to see your skill gaps and improvement recommendations.
          </CardContent>
        </Card>
      )}
    </div>
  );
}
