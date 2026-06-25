"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Briefcase, Clock, Send, TrendingUp, Globe, BarChart3 } from "lucide-react";

interface Stats {
  totalJobs: number;
  pendingApprovals: number;
  appliedThisWeek: number;
  averageMatchScore: number;
}

interface Pipeline {
  byStatus: Record<string, number>;
  byCountry: { country: string; count: number }[];
  bySource: { sourceId: string; sourceName: string; count: number }[];
  totalApplications: number;
  thisWeekApplications: number;
  thisMonthApplications: number;
}

export default function OverviewPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [pipeline, setPipeline] = useState<Pipeline | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get<Stats>("/api/v1/dashboard/stats").catch(() => null),
      api.get<Pipeline>("/api/v1/dashboard/pipeline").catch(() => null),
    ]).then(([s, p]) => {
      setStats(s);
      setPipeline(p);
    }).finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  const cards = [
    { label: "Jobs in feed", value: stats?.totalJobs ?? 0, icon: Briefcase, color: "text-blue-600" },
    { label: "Pending approvals", value: stats?.pendingApprovals ?? 0, icon: Clock, color: "text-yellow-600" },
    { label: "Applied this week", value: stats?.appliedThisWeek ?? 0, icon: Send, color: "text-green-600" },
    { label: "Avg match score", value: `${stats?.averageMatchScore ?? 0}%`, icon: TrendingUp, color: "text-purple-600" },
  ];

  const statusColors: Record<string, string> = {
    discovered: "bg-gray-100",
    pending_approval: "bg-yellow-100",
    approved: "bg-blue-100",
    submitted: "bg-green-100",
    interview: "bg-purple-100",
    rejected: "bg-red-100",
    offer: "bg-emerald-100",
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Dashboard</h1>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {cards.map(({ label, value, icon: Icon, color }) => (
          <Card key={label}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">{label}</CardTitle>
              <Icon className={`h-5 w-5 ${color}`} />
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold">{value}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      {pipeline && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader><CardTitle className="flex items-center gap-2"><BarChart3 className="h-4 w-4" />Pipeline by Status</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              {Object.entries(pipeline.byStatus).map(([status, count]) => (
                <div key={status} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className={`h-3 w-3 rounded-full ${statusColors[status] || "bg-gray-100"}`} />
                    <span className="text-sm capitalize">{status.replace(/_/g, " ")}</span>
                  </div>
                  <span className="text-sm font-medium">{count}</span>
                </div>
              ))}
              {Object.keys(pipeline.byStatus).length === 0 && (
                <p className="text-sm text-muted-foreground">No applications yet</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle className="flex items-center gap-2"><Globe className="h-4 w-4" />Applications by Country</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              {pipeline.byCountry.map(({ country, count }) => (
                <div key={country} className="flex items-center justify-between">
                  <span className="text-sm">{country}</span>
                  <span className="text-sm font-medium">{count}</span>
                </div>
              ))}
              {pipeline.byCountry.length === 0 && (
                <p className="text-sm text-muted-foreground">No data yet</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Summary</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              <div className="flex justify-between">
                <span className="text-sm text-muted-foreground">Total applications</span>
                <span className="font-medium">{pipeline.totalApplications}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-muted-foreground">This week</span>
                <span className="font-medium">{pipeline.thisWeekApplications}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-muted-foreground">This month</span>
                <span className="font-medium">{pipeline.thisMonthApplications}</span>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Applications by Source</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              {pipeline.bySource.map(({ sourceId, sourceName, count }) => (
                <div key={sourceId} className="flex items-center justify-between">
                  <span className="text-sm">{sourceName}</span>
                  <Badge variant="secondary">{count}</Badge>
                </div>
              ))}
              {pipeline.bySource.length === 0 && (
                <p className="text-sm text-muted-foreground">No data yet</p>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
