"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { parseSkills } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ArrowLeft, MapPin, DollarSign, ExternalLink, Zap } from "lucide-react";

interface Job {
  id: string;
  externalJobId: string;
  source: string;
  title: string;
  company: string;
  companyWebsite: string;
  description: string;
  location: string;
  country: string;
  salaryMin: number;
  salaryMax: number;
  currency: string;
  remoteType: string;
  relocationAvailable: boolean;
  visaSponsorshipSignal: boolean;
  seniority: string;
  requiredSkills: string;
  preferredSkills: string;
  experienceYears: number;
  employmentType: string;
  applicationUrl: string;
  atsProvider: string;
  datePosted: string;
  dateDiscovered: string;
}

export default function JobDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [job, setJob] = useState<Job | null>(null);
  const [loading, setLoading] = useState(true);
  const [matching, setMatching] = useState(false);

  useEffect(() => {
    api.get<Job>(`/api/v1/jobs/${id}`).then(setJob).catch(() => router.push("/dashboard/jobs")).finally(() => setLoading(false));
  }, [id, router]);

  const runMatch = async () => {
    setMatching(true);
    try {
      await api.post(`/api/v1/matches`, { jobId: id });
      router.push("/dashboard/matches");
    } catch {}
    setMatching(false);
  };

  if (loading || !job) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

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
                    <Button variant="outline">
                      <ExternalLink className="mr-2 h-4 w-4" />Original post
                    </Button>
                  </a>
                )}
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                {job.remoteType && <Badge variant="outline">{job.remoteType}</Badge>}
                {job.employmentType && <Badge variant="secondary">{job.employmentType}</Badge>}
                {job.seniority && <Badge variant="secondary">{job.seniority}</Badge>}
                {job.source && <Badge variant="outline">{job.source}</Badge>}
              </div>
            </CardContent>
          </Card>

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
              {parseSkills(job.requiredSkills).map((s) => (
                <Badge key={s} variant="outline">{s}</Badge>
              ))}
              {parseSkills(job.requiredSkills).length === 0 && <p className="text-sm text-muted-foreground">No skills extracted</p>}
            </CardContent>
          </Card>

          {parseSkills(job.preferredSkills).length > 0 && (
            <Card>
              <CardHeader><CardTitle>Preferred skills</CardTitle></CardHeader>
              <CardContent className="flex flex-wrap gap-2">
                {parseSkills(job.preferredSkills).map((s) => (
                  <Badge key={s} variant="outline">{s}</Badge>
                ))}
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader><CardTitle>Actions</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              <Button className="w-full" onClick={runMatch} disabled={matching}>
                <Zap className="mr-2 h-4 w-4" />
                {matching ? "Analyzing..." : "Run AI match"}
              </Button>
              <p className="text-xs text-center text-muted-foreground">
                This will analyze your profile against this job and generate a match score.
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
