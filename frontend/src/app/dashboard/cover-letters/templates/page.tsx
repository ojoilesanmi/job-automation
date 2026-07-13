"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { CoverLetterTemplate } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { FileText, Trash2, Plus } from "lucide-react";

export default function CoverLetterTemplatesPage() {
  const [templates, setTemplates] = useState<CoverLetterTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState("");
  const [content, setContent] = useState("");
  const [tone, setTone] = useState("professional");
  const [targetRole, setTargetRole] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [deleting, setDeleting] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<{ templates: CoverLetterTemplate[] }>("/api/v1/cover-letter-templates")
      .then((data) => setTemplates(data.templates))
      .catch(() => setTemplates([]))
      .finally(() => setLoading(false));
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const result = await api.post<CoverLetterTemplate>("/api/v1/cover-letter-templates", {
        name,
        content,
        tone,
        targetRole,
      });
      setTemplates((prev) => [result, ...prev]);
      setName("");
      setContent("");
      setTone("professional");
      setTargetRole("");
      setShowForm(false);
    } catch {}
    setSubmitting(false);
  };

  const handleDelete = async (id: string) => {
    setDeleting(id);
    try {
      await api.del(`/api/v1/cover-letter-templates/${id}`);
      setTemplates((prev) => prev.filter((t) => t.id !== id));
    } catch {}
    setDeleting(null);
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
        <h1 className="text-2xl font-bold">Cover Letter Templates</h1>
        <Button onClick={() => setShowForm(!showForm)}>
          <Plus className="mr-2 h-4 w-4" />
          New template
        </Button>
      </div>

      {showForm && (
        <Card>
          <CardHeader>
            <CardTitle>Create Template</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreate} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">Name</Label>
                <Input
                  id="name"
                  placeholder="e.g. Professional Software Engineer"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="content">Content</Label>
                <Textarea
                  id="content"
                  placeholder="Write your cover letter template here..."
                  className="min-h-[200px]"
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  required
                />
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="tone">Tone</Label>
                  <select
                    id="tone"
                    className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                    value={tone}
                    onChange={(e) => setTone(e.target.value)}
                  >
                    <option value="professional">Professional</option>
                    <option value="enthusiastic">Enthusiastic</option>
                    <option value="concise">Concise</option>
                    <option value="technical">Technical</option>
                  </select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="targetRole">Target Role</Label>
                  <Input
                    id="targetRole"
                    placeholder="e.g. Senior Software Engineer"
                    value={targetRole}
                    onChange={(e) => setTargetRole(e.target.value)}
                  />
                </div>
              </div>
              <div className="flex gap-2">
                <Button type="submit" disabled={submitting}>
                  {submitting ? "Creating..." : "Create template"}
                </Button>
                <Button type="button" variant="outline" onClick={() => setShowForm(false)}>
                  Cancel
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {templates.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            No templates yet. Create one to get started.
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {templates.map((template) => (
            <Card key={template.id} className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-3">
                    <FileText className="mt-1 h-5 w-5 text-muted-foreground" />
                    <div>
                      <h3 className="font-semibold">{template.name}</h3>
                      <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                        <Badge variant="secondary" className="text-xs">
                          {template.tone}
                        </Badge>
                        {template.targetRole && (
                          <Badge variant="outline" className="text-xs">
                            {template.targetRole}
                          </Badge>
                        )}
                        <span>{new Date(template.createdAt).toLocaleDateString()}</span>
                      </div>
                    </div>
                  </div>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => handleDelete(template.id)}
                    disabled={deleting === template.id}
                  >
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </div>
                <div className="mt-4 whitespace-pre-wrap rounded-md bg-muted/50 p-4 text-sm max-h-32 overflow-y-auto">
                  {template.content}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
