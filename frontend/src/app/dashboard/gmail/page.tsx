"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Mail, CheckCircle, AlertCircle } from "lucide-react";

interface GmailStatus { connected: boolean; }
interface ClassifyResult { classified: number; applicationsUpdated: number; }

export default function GmailPage() {
  const [status, setStatus] = useState<GmailStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [classifying, setClassifying] = useState(false);
  const [result, setResult] = useState<ClassifyResult | null>(null);

  useEffect(() => {
    api.get<GmailStatus>("/api/v1/gmail/status").then(setStatus).catch(() => setStatus({ connected: false })).finally(() => setLoading(false));
  }, []);

  const connectGmail = async () => {
    try {
      const data = await api.get<{ url: string }>("/api/v1/gmail/auth-url");
      window.location.href = data.url;
    } catch {}
  };

  const classifyEmails = async () => {
    setClassifying(true);
    try {
      const data = await api.post<ClassifyResult>("/api/v1/gmail/classify");
      setResult(data);
    } catch {}
    setClassifying(false);
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6 max-w-xl">
      <h1 className="text-2xl font-bold">Gmail Integration</h1>

      <Card>
        <CardHeader><CardTitle>Connection Status</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-3">
            {status?.connected ? (
              <><CheckCircle className="h-5 w-5 text-green-600" /><Badge>Connected</Badge></>
            ) : (
              <><AlertCircle className="h-5 w-5 text-yellow-600" /><Badge variant="secondary">Not connected</Badge></>
            )}
          </div>
          {!status?.connected && (
            <Button onClick={connectGmail}><Mail className="mr-2 h-4 w-4" />Connect Gmail</Button>
          )}
        </CardContent>
      </Card>

      {status?.connected && (
        <Card>
          <CardHeader><CardTitle>Email Classifier</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">
              Scan recent emails for interview invites, rejections, assessments, and offers. Application statuses will be auto-updated.
            </p>
            <Button onClick={classifyEmails} disabled={classifying}>
              {classifying ? "Scanning..." : "Scan & classify emails"}
            </Button>
            {result && (
              <div className="rounded-lg bg-muted p-3 text-sm">
                <p>{result.classified} emails classified</p>
                <p>{result.applicationsUpdated} applications updated</p>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
