"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Briefcase, Clock, Send, TrendingUp } from "lucide-react";

interface Stats {
  totalJobs: number;
  pendingApprovals: number;
  appliedThisWeek: number;
  averageMatchScore: number;
}

export default function OverviewPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<Stats>("/api/v1/dashboard/stats")
      .then(setStats)
      .catch(() => setStats({ totalJobs: 0, pendingApprovals: 0, appliedThisWeek: 0, averageMatchScore: 0 }))
      .finally(() => setLoading(false));
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

      <Card>
        <CardHeader>
          <CardTitle>Recent matches</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            No matches yet. Set up your preferences and upload your CV to get started.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
