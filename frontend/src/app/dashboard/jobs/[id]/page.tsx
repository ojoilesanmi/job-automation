"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { parseSkills } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { ArrowLeft, MapPin, DollarSign, ExternalLink, Zap, AlertTriangle, CheckCircle, XCircle, FileText, Clock } from "lucide-react";

interface Job {
  id: string;
  title: string;
  company: string;
  description: string;
  location: string;
  country: string;
  salaryMin: number;
  salaryMax: number;
  currency: string;
  remoteType: string;
  seniority: string;
  requiredSkills: string;
  preferredSkills: string;
  experienceYears: number;
  employmentType: string;
  applicationUrl: string;
}

interface Match {
  id: string;
  jobId: string;
  fitScore: number;
  skillsScore: number;
  experienceScore: number;
  roleScore: number;
  locationScore: number;
  matchedSkills: string;
  missingSkills: string;
  reasonsToApply: string;
  reasonsToSkip: string;
  riskFlags: string;
  status: string;
}

export default function JobDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [job, setJob] = useState<Job | null>(null);
  const [match, setMatch] = useState<Match | null>(null);
  const [loading, setLoading] = useState(true);
  const [matching, setMatching] = useState(false);
  const [tone, setTone] = useState("professional");
  const [coverLetter, setCoverLetter] = useState<string | null>(null);
  const [generatingCL, setGeneratingCL] = useState(false);
  const [followUpDate, setFollowUpDate] = useState("");
  const [schedulingFollowUp, setSchedulingFollowUp] = useState(false);

  useEffect(() => {
    Promise.all([
      api.get<Job>(`/api/v1/jobs/${id}`).catch(() => null),
      api.get<{ matches: Match[] }>(`/api/v1/matches`).catch(() => ({ matches: [] })),
    ]).then(([jobData, matchData]) => {
      setJob(jobData);
      const existing = matchData?.matches?.find((m) => m.jobId === id);
      if (existing) setMatch(existing);
    }).finally(() => setLoading(false));
  }, [id]);

  const runMatch = async () => {
    setMatching(true);
    try {
      const result = await api.post<Match>("/api/v1/matches", { jobId: id });
      setMatch(result);
    } catch {}
    setMatching(false);
  };

  const generateCoverLetter = async () => {
    setGeneratingCL(true);
    try {
      const result = await api.post<{ content: string }>(`/api/v1/jobs/${id}/cover-letters/generate`, { tone });
      setCoverLetter(result.content);
    } catch {}
    setGeneratingCL(false);
  };

  const scheduleFollowUp = async () => {
    if (!followUpDate || !match) return;
    setSchedulingFollowUp(true);
    try {
      await api.post("/api/v1/follow-ups", {
        applicationId: match.id,
        nextFollowUpAt: new Date(followUpDate).toISOString(),
      });
    } catch {}
    setSchedulingFollowUp(false);
  };

  if (loading || !job) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  const required = parseSkills(job.requiredSkills);
  const preferred = parseSkills(job.preferredSkills);
  const matched = match ? parseSkills(match.matchedSkills) : [];
  const missing = match ? parseSkills(match.missingSkills) : [];
  const riskFlags = match ? parseSkills(match.riskFlags) : [];
  const reasonsToApply = match ? parseSkills(match.reasonsToApply) : [];
  const reasonsToSkip = match ? parseSkills(match.reasonsToSkip) : [];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => router.back()}><ArrowLeft className="h-5 w-5" /></Button>
        <h1 className="text-2xl font-bold">{job.title}</h1>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardContent className="p-6">
              <div className="flex items-start justify-between">
                <div>
                  <h2 className="text-xl font-semibold">{job.company}</h2>
                  <div className="mt-2 flex flex-wrap gap-2 text-sm text-muted-foreground">
                    {job.location && <span className="flex items-center gap-1"><MapPin className="h-4 w-4" />{job.location}</span>}
                    {(job.salaryMin || job.salaryMax) && (
                      <span className="flex items-center gap-1">
                        <DollarSign className="h-4 w-4" />
                        {job.salaryMin && job.salaryMax ? `$${(job.salaryMin / 1000).toFixed(0)}k–$${(job.salaryMax / 1000).toFixed(0)}k` : "Disclosed"}
                      </span>
                    )}
                  </div>
                </div>
                {job.applicationUrl && (
                  <a href={job.applicationUrl} target="_blank" rel="noopener noreferrer">
                    <Button variant="outline"><ExternalLink className="mr-2 h-4 w-4" />Original post</Button>
                  </a>
                )}
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                {job.remoteType && <Badge variant="outline">{job.remoteType}</Badge>}
                {job.employmentType && <Badge variant="secondary">{job.employmentType}</Badge>}
                {job.seniority && <Badge variant="secondary">{job.seniority}</Badge>}
              </div>
            </CardContent>
          </Card>

          {match && (
            <Card>
              <CardHeader><CardTitle>Fit Breakdown</CardTitle></CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center gap-3">
                  <span className="text-3xl font-bold">{match.fitScore}%</span>
                  <span className="text-sm text-muted-foreground">Overall match</span>
                </div>
                {[
                  { label: "Skills", score: match.skillsScore },
                  { label: "Experience", score: match.experienceScore },
                  { label: "Role", score: match.roleScore },
                  { label: "Location", score: match.locationScore },
                ].map(({ label, score }) => (
                  <div key={label} className="space-y-1">
                    <div className="flex justify-between text-sm">
                      <span>{label}</span>
                      <span className="font-medium">{score}%</span>
                    </div>
                    <Progress value={score} className="h-2" />
                  </div>
                ))}
              </CardContent>
            </Card>
          )}

          {match && reasonsToApply.length > 0 && (
            <Card>
              <CardHeader><CardTitle className="flex items-center gap-2"><CheckCircle className="h-5 w-5 text-green-600" />Reasons to Apply</CardTitle></CardHeader>
              <CardContent>
                <ul className="space-y-2">
                  {reasonsToApply.map((r, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm">
                      <CheckCircle className="h-4 w-4 mt-0.5 text-green-600 shrink-0" />
                      {r}
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          )}

          {match && reasonsToSkip.length > 0 && (
            <Card>
              <CardHeader><CardTitle className="flex items-center gap-2"><AlertTriangle className="h-5 w-5 text-yellow-600" />Reasons to Skip</CardTitle></CardHeader>
              <CardContent>
                <ul className="space-y-2">
                  {reasonsToSkip.map((r, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm">
                      <AlertTriangle className="h-4 w-4 mt-0.5 text-yellow-600 shrink-0" />
                      {r}
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          )}

          {match && riskFlags.length > 0 && (
            <Card className="border-red-200">
              <CardHeader><CardTitle className="flex items-center gap-2"><XCircle className="h-5 w-5 text-red-600" />Risk Flags</CardTitle></CardHeader>
              <CardContent>
                <ul className="space-y-2">
                  {riskFlags.map((r, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm">
                      <XCircle className="h-4 w-4 mt-0.5 text-red-600 shrink-0" />
                      {r}
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader><CardTitle>Description</CardTitle></CardHeader>
            <CardContent>
              <div className="prose prose-sm max-w-none whitespace-pre-wrap">{job.description}</div>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader><CardTitle>Required skills</CardTitle></CardHeader>
            <CardContent className="flex flex-wrap gap-2">
              {required.map((s) => (
                <Badge key={s} variant={matched.includes(s) ? "default" : "outline"}>
                  {matched.includes(s) && <CheckCircle className="mr-1 h-3 w-3" />}{s}
                </Badge>
              ))}
              {required.length === 0 && <p className="text-sm text-muted-foreground">No skills extracted</p>}
            </CardContent>
          </Card>

          {preferred.length > 0 && (
            <Card>
              <CardHeader><CardTitle>Preferred skills</CardTitle></CardHeader>
              <CardContent className="flex flex-wrap gap-2">
                {preferred.map((s) => (
                  <Badge key={s} variant={matched.includes(s) ? "default" : "outline"}>
                    {matched.includes(s) && <CheckCircle className="mr-1 h-3 w-3" />}{s}
                  </Badge>
                ))}
              </CardContent>
            </Card>
          )}

          {missing.length > 0 && (
            <Card className="border-yellow-200">
              <CardHeader><CardTitle className="text-yellow-700">Missing skills</CardTitle></CardHeader>
              <CardContent className="flex flex-wrap gap-2">
                {missing.map((s) => (
                  <Badge key={s} variant="destructive">{s}</Badge>
                ))}
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader><CardTitle>Actions</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {!match ? (
                <Button className="w-full" onClick={runMatch} disabled={matching}>
                  <Zap className="mr-2 h-4 w-4" />
                  {matching ? "Analyzing..." : "Run AI match"}
                </Button>
              ) : (
                <Button className="w-full" onClick={() => router.push("/dashboard/matches")}>
                  View match details
                </Button>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Cover Letter</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              <select
                className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                value={tone}
                onChange={(e) => setTone(e.target.value)}
              >
                <option value="professional">Professional</option>
                <option value="enthusiastic">Enthusiastic</option>
                <option value="concise">Concise</option>
                <option value="technical">Technical</option>
              </select>
              <Button className="w-full" variant="outline" onClick={generateCoverLetter} disabled={generatingCL}>
                <FileText className="mr-2 h-4 w-4" />
                {generatingCL ? "Generating..." : "Generate cover letter"}
              </Button>
              {coverLetter && (
                <div className="mt-3 rounded-md bg-muted p-3 text-sm whitespace-pre-wrap max-h-64 overflow-y-auto">{coverLetter}</div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle className="flex items-center gap-2"><Clock className="h-4 w-4" />Follow-up</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              <input
                type="datetime-local"
                className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                value={followUpDate}
                onChange={(e) => setFollowUpDate(e.target.value)}
              />
              <Button className="w-full" variant="outline" onClick={scheduleFollowUp} disabled={schedulingFollowUp || !followUpDate}>
                {schedulingFollowUp ? "Scheduling..." : "Schedule follow-up"}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
