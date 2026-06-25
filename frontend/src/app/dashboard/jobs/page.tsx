"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { parseSkills } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Briefcase, MapPin, DollarSign, Search, ExternalLink, Filter } from "lucide-react";

interface Job {
  id: string;
  title: string;
  company: string;
  location: string;
  country: string;
  salaryMin: number;
  salaryMax: number;
  remoteType: string;
  seniority: string;
  requiredSkills: string;
  employmentType: string;
  applicationUrl: string;
  source: string;
}

interface JobsResponse {
  jobs: Job[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

const REMOTE_OPTIONS = ["", "full_remote", "hybrid", "onsite"];
const SENIORITY_OPTIONS = ["", "junior", "mid", "senior", "lead", "executive"];
const COUNTRY_OPTIONS = ["", "US", "UK", "Germany", "Canada", "Nigeria", "Remote"];

export default function JobsPage() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [totalElements, setTotalElements] = useState(0);
  const [showFilters, setShowFilters] = useState(false);
  const [remoteFilter, setRemoteFilter] = useState("");
  const [seniorityFilter, setSeniorityFilter] = useState("");
  const [countryFilter, setCountryFilter] = useState("");

  useEffect(() => {
    api.get<JobsResponse>("/api/v1/jobs")
      .then((data) => { setJobs(data.jobs); setTotalElements(data.totalElements); })
      .catch(() => setJobs([]))
      .finally(() => setLoading(false));
  }, []);

  const filtered = jobs.filter((j) => {
    const matchSearch = !search || j.title.toLowerCase().includes(search.toLowerCase()) || j.company.toLowerCase().includes(search.toLowerCase());
    const matchRemote = !remoteFilter || j.remoteType === remoteFilter;
    const matchSeniority = !seniorityFilter || j.seniority === seniorityFilter;
    const matchCountry = !countryFilter || j.country === countryFilter;
    return matchSearch && matchRemote && matchSeniority && matchCountry;
  });

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Job Feed</h1>
        <p className="text-sm text-muted-foreground">{totalElements} jobs found</p>
      </div>

      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-md">
          <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
          <Input placeholder="Search jobs..." className="pl-9" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <Button variant="outline" size="sm" onClick={() => setShowFilters(!showFilters)}>
          <Filter className="mr-2 h-4 w-4" />Filters
        </Button>
      </div>

      {showFilters && (
        <div className="flex flex-wrap gap-3 rounded-lg border bg-card p-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-muted-foreground">Remote</label>
            <select className="rounded-md border bg-background px-3 py-1.5 text-sm" value={remoteFilter} onChange={(e) => setRemoteFilter(e.target.value)}>
              {REMOTE_OPTIONS.map((o) => <option key={o} value={o}>{o || "All"}</option>)}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-muted-foreground">Seniority</label>
            <select className="rounded-md border bg-background px-3 py-1.5 text-sm" value={seniorityFilter} onChange={(e) => setSeniorityFilter(e.target.value)}>
              {SENIORITY_OPTIONS.map((o) => <option key={o} value={o}>{o || "All"}</option>)}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-muted-foreground">Country</label>
            <select className="rounded-md border bg-background px-3 py-1.5 text-sm" value={countryFilter} onChange={(e) => setCountryFilter(e.target.value)}>
              {COUNTRY_OPTIONS.map((o) => <option key={o} value={o}>{o || "All"}</option>)}
            </select>
          </div>
          <div className="flex items-end">
            <Button variant="ghost" size="sm" onClick={() => { setRemoteFilter(""); setSeniorityFilter(""); setCountryFilter(""); }}>Clear</Button>
          </div>
        </div>
      )}

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
                          <Button variant="ghost" size="icon"><ExternalLink className="h-4 w-4" /></Button>
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
