"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ScrollText, ChevronLeft, ChevronRight } from "lucide-react";

interface AuditLog {
  id: string;
  userId: string;
  userEmail: string;
  action: string;
  entityType: string;
  entityId: string;
  metadata: Record<string, unknown>;
  createdAt: string;
}

interface AuditLogsResponse {
  logs: AuditLog[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

const actionColors: Record<string, string> = {
  "application.created": "bg-blue-100 text-blue-800",
  "application.status_changed": "bg-yellow-100 text-yellow-800",
  "application.submitted": "bg-green-100 text-green-800",
  "profile.updated": "bg-purple-100 text-purple-800",
  "auth.login": "bg-gray-100 text-gray-800",
  "auth.register": "bg-gray-100 text-gray-800",
};

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    setLoading(true);
    api.get<AuditLogsResponse>(`/api/v1/admin/audit-logs?page=${page}&size=50`)
      .then((data) => { setLogs(data.logs); setTotalPages(data.totalPages); setTotal(data.totalElements); })
      .catch(() => setLogs([]))
      .finally(() => setLoading(false));
  }, [page]);

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-2xl font-bold"><ScrollText className="h-6 w-6" />Audit Logs</h1>
        <p className="text-sm text-muted-foreground">{total} entries</p>
      </div>

      {logs.length === 0 ? (
        <Card><CardContent className="py-12 text-center text-muted-foreground">No audit logs found.</CardContent></Card>
      ) : (
        <Card>
          <CardContent className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-muted-foreground">
                  <th className="p-3">Time</th>
                  <th className="p-3">User</th>
                  <th className="p-3">Action</th>
                  <th className="p-3">Entity</th>
                  <th className="p-3">Details</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id} className="border-b">
                    <td className="p-3 text-xs text-muted-foreground whitespace-nowrap">
                      {log.createdAt ? new Date(log.createdAt).toLocaleString() : "—"}
                    </td>
                    <td className="p-3 text-xs">{log.userEmail || "system"}</td>
                    <td className="p-3">
                      <Badge variant="outline" className={`text-xs ${actionColors[log.action] || ""}`}>
                        {log.action}
                      </Badge>
                    </td>
                    <td className="p-3 text-xs text-muted-foreground">
                      {log.entityType}{log.entityId ? ` / ${log.entityId.slice(0, 8)}...` : ""}
                    </td>
                    <td className="p-3 text-xs text-muted-foreground max-w-xs truncate">
                      {log.metadata ? JSON.stringify(log.metadata) : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-4">
          <Button variant="outline" size="sm" onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0}>
            <ChevronLeft className="h-4 w-4" />Previous
          </Button>
          <span className="text-sm text-muted-foreground">Page {page + 1} of {totalPages}</span>
          <Button variant="outline" size="sm" onClick={() => setPage(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1}>
            Next<ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}
    </div>
  );
}
