"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Target, Clock, CheckCircle, Send, XCircle, ArrowRight } from "lucide-react";

interface JobSummary {
  id: string;
  title: string;
  company: string;
}

interface Application {
  id: string;
  jobId: string;
  userId: string;
  status: string;
  notes: string;
  job: JobSummary | null;
  coverLetter: string | null;
}

interface ApplicationsResponse {
  applications: Application[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

const statusConfig: Record<string, { label: string; variant: string; icon: React.ComponentType<{ className?: string }> }> = {
  draft: { label: "Draft", variant: "secondary", icon: Clock },
  pending_approval: { label: "Pending approval", variant: "warning", icon: Clock },
  approved: { label: "Approved", variant: "success", icon: CheckCircle },
  submitted: { label: "Submitted", variant: "default", icon: Send },
  rejected: { label: "Rejected", variant: "destructive", icon: XCircle },
};

export default function ApplicationsPage() {
  const [applications, setApplications] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<ApplicationsResponse>("/api/v1/applications")
      .then((data) => setApplications(data.applications))
      .catch(() => setApplications([]))
      .finally(() => setLoading(false));
  }, []);

  const approve = async (id: string) => {
    try {
      const updated = await api.put<Application>(`/api/v1/applications/${id}/status`, { status: "approved" });
      setApplications(applications.map((a) => (a.id === id ? updated : a)));
    } catch {}
  };

  const submit = async (id: string) => {
    try {
      const updated = await api.post<Application>(`/api/v1/applications/${id}/submit`, {});
      setApplications(applications.map((a) => (a.id === id ? updated : a)));
    } catch {}
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Applications</h1>

      {applications.length === 0 ? (
        <Card><CardContent className="py-12 text-center text-muted-foreground">
          No applications yet. Match some jobs first to create applications.
        </CardContent></Card>
      ) : (
        <div className="space-y-3">
          {applications.map((app) => {
            const cfg = statusConfig[app.status] || statusConfig.draft;
            const Icon = cfg.icon;
            return (
              <Card key={app.id}>
                <CardContent className="flex items-center gap-4 p-4">
                  <Icon className="h-5 w-5 text-muted-foreground" />
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="font-medium">{app.job?.title || "Unknown job"}</h3>
                      <span className="text-sm text-muted-foreground">&middot; {app.job?.company || ""}</span>
                    </div>
                    {app.coverLetter && (
                      <p className="text-xs text-muted-foreground line-clamp-1">Cover letter attached</p>
                    )}
                  </div>
                  <Badge variant={cfg.variant as any}>{cfg.label}</Badge>
                  <div className="flex gap-2">
                    {app.status === "pending_approval" && (
                      <>
                        <Button size="sm" onClick={() => approve(app.id)}>Approve</Button>
                        <Button size="sm" variant="destructive">Reject</Button>
                      </>
                    )}
                    {app.status === "approved" && (
                      <Button size="sm" onClick={() => submit(app.id)}>
                        <Send className="mr-2 h-3 w-3" />Submit
                      </Button>
                    )}
                    <Link href={`/dashboard/jobs/${app.jobId}`}>
                      <Button size="sm" variant="ghost"><ArrowRight className="h-4 w-4" /></Button>
                    </Link>
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
