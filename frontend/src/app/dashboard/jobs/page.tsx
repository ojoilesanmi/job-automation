"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { parseSkills } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Briefcase, MapPin, DollarSign, Search, ExternalLink } from "lucide-react";

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

interface JobsResponse {
  jobs: Job[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export default function JobsPage() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    api.get<JobsResponse>("/api/v1/jobs")
      .then((data) => {
        setJobs(data.jobs);
        setTotalElements(data.totalElements);
      })
      .catch(() => setJobs([]))
      .finally(() => setLoading(false));
  }, []);

  const filtered = jobs.filter((j) =>
    !search || j.title.toLowerCase().includes(search.toLowerCase()) || j.company.toLowerCase().includes(search.toLowerCase())
  );

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Job Feed</h1>
        <p className="text-sm text-muted-foreground">{totalElements} jobs found</p>
      </div>

      <div className="relative max-w-md">
        <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
        <Input placeholder="Search jobs..." className="pl-9" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      {filtered.length === 0 ? (
        <Card><CardContent className="py-12 text-center text-muted-foreground">No jobs found. Run a job discovery worker to populate the feed.</CardContent></Card>
      ) : (
        <div className="space-y-3">
          {filtered.map((job) => (
            <Link key={job.id} href={`/dashboard/jobs/${job.id}`}>
              <Card className="cursor-pointer transition-colors hover:bg-accent/50">
                <CardContent className="flex items-start gap-4 p-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded bg-muted text-sm font-bold">
                    {job.company.charAt(0)}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="font-semibold">{job.title}</h3>
                        <p className="text-sm text-muted-foreground">{job.company}</p>
                      </div>
                      {job.applicationUrl && (
                        <a href={job.applicationUrl} target="_blank" rel="noopener noreferrer" onClick={(e) => e.stopPropagation()}>
                          <Button variant="ghost" size="icon">
                            <ExternalLink className="h-4 w-4" />
                          </Button>
                        </a>
                      )}
                    </div>
                    <div className="mt-2 flex flex-wrap gap-2 text-xs text-muted-foreground">
                      {job.location && <span className="flex items-center gap-1"><MapPin className="h-3 w-3" />{job.location}</span>}
                      {(job.salaryMin || job.salaryMax) && (
                        <span className="flex items-center gap-1">
                          <DollarSign className="h-3 w-3" />
                          {job.salaryMin && job.salaryMax ? `$${(job.salaryMin / 1000).toFixed(0)}k–$${(job.salaryMax / 1000).toFixed(0)}k` : "Disclosed"}
                        </span>
                      )}
                      {job.remoteType && <Badge variant="outline">{job.remoteType}</Badge>}
                      {job.employmentType && <Badge variant="secondary">{job.employmentType}</Badge>}
                    </div>
                    <div className="mt-2 flex flex-wrap gap-1">
                      {parseSkills(job.requiredSkills).slice(0, 5).map((s) => (
                        <Badge key={s} variant="outline" className="text-xs">{s}</Badge>
                      ))}
                      {parseSkills(job.requiredSkills).length > 5 && <Badge variant="outline" className="text-xs">+{parseSkills(job.requiredSkills).length - 5}</Badge>}
                    </div>
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
