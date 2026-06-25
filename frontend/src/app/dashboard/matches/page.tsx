"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Zap,
  CheckCircle,
  XCircle,
  Bookmark,
  MapPin,
  Clock,
  ArrowRight,
  Star,
} from "lucide-react";

interface JobMatch {
  id: string;
  jobId: string;
  jobTitle: string;
  company: string;
  fitScore: number;
  skillsScore: number;
  experienceScore: number;
  roleScore: number;
  locationScore: number;
  salaryScore: number;
  matchedSkills: string[];
  missingSkills: string[];
  reasonsToApply: string[];
  reasonsToSkip: string[];
  status: string;
  createdAt: string;
}

interface MatchesResponse {
  matches: JobMatch[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

const statusConfig: Record<string, { label: string; color: string }> = {
  scored: { label: "New", color: "bg-blue-100 text-blue-800" },
  pending: { label: "Pending", color: "bg-yellow-100 text-yellow-800" },
  approved: { label: "Approved", color: "bg-green-100 text-green-800" },
  rejected: { label: "Rejected", color: "bg-red-100 text-red-800" },
  saved: { label: "Saved", color: "bg-purple-100 text-purple-800" },
};

function scoreColor(score: number) {
  if (score >= 80) return "text-green-600";
  if (score >= 60) return "text-yellow-600";
  return "text-red-600";
}

function scoreBadgeVariant(score: number) {
  if (score >= 80) return "default";
  if (score >= 60) return "secondary";
  return "destructive";
}

export default function MatchesPage() {
  const router = useRouter();
  const [matches, setMatches] = useState<JobMatch[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  useEffect(() => {
    loadMatches();
  }, [statusFilter]);

  const loadMatches = () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (statusFilter) params.set("status", statusFilter);
    params.set("size", "50");

    api
      .get<MatchesResponse>(`/api/v1/matches?${params.toString()}`)
      .then((data) => setMatches(data.matches))
      .catch(() => setMatches([]))
      .finally(() => setLoading(false));
  };

  const approve = async (id: string) => {
    setActionLoading(id);
    try {
      await api.post(`/api/v1/matches/${id}/approve`);
      loadMatches();
    } catch {}
    setActionLoading(null);
  };

  const reject = async (id: string) => {
    setActionLoading(id);
    try {
      await api.post(`/api/v1/matches/${id}/reject`);
      loadMatches();
    } catch {}
    setActionLoading(null);
  };

  const save = async (id: string) => {
    setActionLoading(id);
    try {
      await api.post(`/api/v1/matches/${id}/save`);
      loadMatches();
    } catch {}
    setActionLoading(null);
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
        <h1 className="text-2xl font-bold">Matches</h1>
        <div className="flex gap-2">
          {["", "scored", "approved", "rejected", "saved"].map((s) => (
            <Button
              key={s}
              variant={statusFilter === s ? "default" : "outline"}
              size="sm"
              onClick={() => setStatusFilter(s)}
            >
              {s === "" ? "All" : statusConfig[s]?.label || s}
            </Button>
          ))}
        </div>
      </div>

      {matches.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            No matches found. Go to Jobs and click &quot;Run AI match&quot; to analyze
            your profile against a job.
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {matches.map((match) => {
            const st = statusConfig[match.status] || statusConfig.scored;
            const score = Math.round(match.fitScore * 100);

            return (
              <Card key={match.id} className="hover:shadow-md transition-shadow">
                <CardContent className="p-6">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3">
                        <h3 className="text-lg font-semibold">
                          {match.jobTitle}
                        </h3>
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${st.color}`}
                        >
                          {st.label}
                        </span>
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {match.company}
                      </p>

                      <div className="mt-3 flex flex-wrap gap-4 text-sm text-muted-foreground">
                        {match.createdAt && (
                          <span className="flex items-center gap-1">
                            <Clock className="h-3.5 w-3.5" />
                            {new Date(match.createdAt).toLocaleDateString()}
                          </span>
                        )}
                      </div>

                      <div className="mt-3 flex flex-wrap gap-2">
                        {match.matchedSkills?.slice(0, 5).map((s) => (
                          <Badge key={s} variant="outline" className="text-xs">
                            {s}
                          </Badge>
                        ))}
                        {match.matchedSkills && match.matchedSkills.length > 5 && (
                          <Badge variant="outline" className="text-xs">
                            +{match.matchedSkills.length - 5} more
                          </Badge>
                        )}
                      </div>

                      {match.reasonsToApply && match.reasonsToApply.length > 0 && (
                        <div className="mt-3">
                          <p className="text-xs font-medium text-green-700 mb-1">
                            Reasons to apply:
                          </p>
                          <ul className="text-xs text-muted-foreground space-y-0.5">
                            {match.reasonsToApply.slice(0, 2).map((r, i) => (
                              <li key={i} className="flex items-start gap-1">
                                <CheckCircle className="h-3 w-3 mt-0.5 text-green-500 shrink-0" />
                                {r}
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}

                      {match.missingSkills && match.missingSkills.length > 0 && (
                        <div className="mt-2">
                          <p className="text-xs font-medium text-red-700 mb-1">
                            Missing skills:
                          </p>
                          <div className="flex flex-wrap gap-1">
                            {match.missingSkills.map((s) => (
                              <Badge
                                key={s}
                                variant="outline"
                                className="text-xs text-red-600 border-red-200"
                              >
                                {s}
                              </Badge>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>

                    <div className="ml-6 flex flex-col items-end gap-3">
                      <div
                        className={`text-3xl font-bold ${scoreColor(score)}`}
                      >
                        {score}%
                      </div>
                      <div className="flex gap-2">
                        {match.status === "scored" && (
                          <>
                            <Button
                              size="sm"
                              onClick={() => approve(match.id)}
                              disabled={actionLoading === match.id}
                              className="bg-green-600 hover:bg-green-700"
                            >
                              {actionLoading === match.id ? (
                                <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                              ) : (
                                <>
                                  <CheckCircle className="mr-1 h-3.5 w-3.5" />
                                  Approve
                                </>
                              )}
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => save(match.id)}
                              disabled={actionLoading === match.id}
                            >
                              <Bookmark className="mr-1 h-3.5 w-3.5" />
                              Save
                            </Button>
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => reject(match.id)}
                              disabled={actionLoading === match.id}
                            >
                              <XCircle className="mr-1 h-3.5 w-3.5" />
                              Reject
                            </Button>
                          </>
                        )}
                        {match.status === "approved" && (
                          <Badge variant="default" className="bg-green-600">
                            <CheckCircle className="mr-1 h-3.5 w-3.5" />
                            Approved
                          </Badge>
                        )}
                        {match.status === "rejected" && (
                          <Badge variant="destructive">
                            <XCircle className="mr-1 h-3.5 w-3.5" />
                            Rejected
                          </Badge>
                        )}
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() =>
                            router.push(`/dashboard/jobs/${match.jobId}`)
                          }
                        >
                          <ArrowRight className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
