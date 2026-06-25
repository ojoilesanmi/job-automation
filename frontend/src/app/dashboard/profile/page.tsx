"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Plus, X, Upload, FileText, Check, Trash2 } from "lucide-react";

interface Experience {
  id: string;
  company: string;
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  achievements: string;
}

interface Project {
  id: string;
  name: string;
  description: string;
  url: string;
  technologies: string;
}

interface Skill {
  id: string;
  skillName: string;
  skillType: string;
  proficiency: string;
  yearsUsed: number | null;
}

interface CvDocument {
  id: string;
  fileName: string;
  fileUrl: string;
  parsedText: string | null;
  versionName: string | null;
  isDefault: boolean;
  createdAt: string;
}

interface Profile {
  id: string;
  headline: string;
  summary: string;
  location: string;
  yearsOfExperience: number;
  primaryRole: string;
  skills: Skill[];
  experiences: Experience[];
  projects: Project[];
  createdAt: string;
  updatedAt: string;
}

export default function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [newSkill, setNewSkill] = useState("");
  const [saved, setSaved] = useState(false);
  const savedTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [cvs, setCvs] = useState<CvDocument[]>([]);
  const [cvUploading, setCvUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [expForm, setExpForm] = useState({ company: "", title: "", description: "", startDate: "", endDate: "", achievements: "" });
  const [projForm, setProjForm] = useState({ name: "", description: "", url: "", technologies: "" });

  useEffect(() => {
    Promise.all([
      api.get<Profile>("/api/v1/profile").catch(() => null),
      api.get<CvDocument[]>("/api/v1/cvs").catch(() => []),
    ]).then(([p, c]) => {
      setProfile(p);
      setCvs(c);
    }).finally(() => setLoading(false));
  }, []);

  const refreshCvs = () => api.get<CvDocument[]>("/api/v1/cvs").then(setCvs).catch(() => {});

  const handleFileUpload = useCallback(async (file: File) => {
    const allowed = [".pdf", ".doc", ".docx", ".txt"];
    const ext = "." + file.name.split(".").pop()?.toLowerCase();
    if (!allowed.includes(ext)) return;
    setCvUploading(true);
    try {
      await api.upload<CvDocument>("/api/v1/cvs/upload", file);
      await refreshCvs();
    } catch {}
    setCvUploading(false);
  }, []);

  const onFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFileUpload(file);
    e.target.value = "";
  };

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFileUpload(file);
  };

  const setDefaultCv = async (id: string) => {
    await api.put(`/api/v1/cvs/${id}/default`);
    await refreshCvs();
  };

  const deleteCv = async (id: string) => {
    await api.del(`/api/v1/cvs/${id}`);
    await refreshCvs();
  };

  const saveProfile = async () => {
    if (!profile) return;
    setSaving(true);
    setSaved(false);
    try {
      const updated = await api.put<Profile>("/api/v1/profile", profile);
      setProfile(updated);
      await api.put("/api/v1/profile/skills", {
        skills: profile.skills.map((s) => ({
          skillName: s.skillName,
          skillType: s.skillType || "technical",
          proficiency: s.proficiency || "intermediate",
          yearsUsed: s.yearsUsed || null,
        })),
      });
      setSaved(true);
      if (savedTimeout.current) clearTimeout(savedTimeout.current);
      savedTimeout.current = setTimeout(() => setSaved(false), 2000);
    } catch {}
    setSaving(false);
  };

  const addSkill = () => {
    if (newSkill.trim() && profile) {
      setProfile({
        ...profile,
        skills: [...profile.skills, { id: "", skillName: newSkill.trim(), skillType: "technical", proficiency: "intermediate", yearsUsed: null }],
      });
      setNewSkill("");
    }
  };

  const removeSkill = (skillName: string) => {
    if (!profile) return;
    setProfile({ ...profile, skills: profile.skills.filter((s) => s.skillName !== skillName) });
  };

  const saveExperience = async () => {
    if (!profile) return;
    const allExps = [
      ...profile.experiences.map((e) => ({
        company: e.company, title: e.title, description: e.description || null,
        startDate: e.startDate || null, endDate: e.endDate || null, achievements: e.achievements || null,
      })),
      ...(expForm.company && expForm.title ? [{
        company: expForm.company, title: expForm.title, description: expForm.description || null,
        startDate: expForm.startDate || null, endDate: expForm.endDate || null, achievements: expForm.achievements || null,
      }] : []),
    ];
    if (allExps.length === 0) return;
    try {
      await api.put("/api/v1/profile/experience", { experiences: allExps });
      const updated = await api.get<Profile>("/api/v1/profile");
      setProfile(updated);
      setExpForm({ company: "", title: "", description: "", startDate: "", endDate: "", achievements: "" });
    } catch {}
  };

  const removeExperience = async (index: number) => {
    if (!profile) return;
    const allExps = profile.experiences
      .filter((_, i) => i !== index)
      .map((e) => ({
        company: e.company, title: e.title, description: e.description || null,
        startDate: e.startDate || null, endDate: e.endDate || null, achievements: e.achievements || null,
      }));
    try {
      await api.put("/api/v1/profile/experience", { experiences: allExps });
      const updated = await api.get<Profile>("/api/v1/profile");
      setProfile(updated);
    } catch {}
  };

  const saveProject = async () => {
    if (!profile) return;
    const allProjs = [
      ...profile.projects.map((p) => ({
        name: p.name, description: p.description || null,
        technologies: p.technologies || null, url: p.url || null,
      })),
      ...(projForm.name ? [{
        name: projForm.name, description: projForm.description || null,
        technologies: projForm.technologies || null, url: projForm.url || null,
      }] : []),
    ];
    if (allProjs.length === 0) return;
    try {
      await api.put("/api/v1/profile/projects", { projects: allProjs });
      const updated = await api.get<Profile>("/api/v1/profile");
      setProfile(updated);
      setProjForm({ name: "", description: "", url: "", technologies: "" });
    } catch {}
  };

  const removeProject = async (index: number) => {
    if (!profile) return;
    const allProjs = profile.projects
      .filter((_, i) => i !== index)
      .map((p) => ({
        name: p.name, description: p.description || null,
        technologies: p.technologies || null, url: p.url || null,
      }));
    try {
      await api.put("/api/v1/profile/projects", { projects: allProjs });
      const updated = await api.get<Profile>("/api/v1/profile");
      setProfile(updated);
    } catch {}
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  if (!profile) {
    return <div className="flex h-64 items-center justify-center text-muted-foreground">Failed to load profile.</div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Profile</h1>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Professional information</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label>Headline</Label>
              <Input value={profile.headline ?? ""} onChange={(e) => setProfile({ ...profile, headline: e.target.value })} placeholder="e.g. Senior Software Engineer" />
            </div>
            <div className="space-y-2">
              <Label>Primary role</Label>
              <Input value={profile.primaryRole ?? ""} onChange={(e) => setProfile({ ...profile, primaryRole: e.target.value })} placeholder="e.g. Backend Developer" />
            </div>
            <div className="space-y-2">
              <Label>Location</Label>
              <Input value={profile.location ?? ""} onChange={(e) => setProfile({ ...profile, location: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Years of experience</Label>
              <Input type="number" value={profile.yearsOfExperience ?? 0} onChange={(e) => setProfile({ ...profile, yearsOfExperience: +e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Summary</Label>
              <Textarea rows={4} value={profile.summary ?? ""} onChange={(e) => setProfile({ ...profile, summary: e.target.value })} />
            </div>
            <Button onClick={saveProfile} disabled={saving}>
              {saving ? (
                <><span className="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />Saving...</>
              ) : saved ? (
                <><Check className="mr-1.5 h-4 w-4" />Saved</>
              ) : "Save profile"}
            </Button>
          </CardContent>
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader><CardTitle>Skills</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2">
                <Input placeholder="Add a skill" value={newSkill} onChange={(e) => setNewSkill(e.target.value)} onKeyDown={(e) => e.key === "Enter" && addSkill()} />
                <Button size="icon" onClick={addSkill}><Plus className="h-4 w-4" /></Button>
              </div>
              <div className="flex flex-wrap gap-2">
                {profile.skills.map((s) => (
                  <Badge key={s.skillName} variant="secondary" className="gap-1">
                    {s.skillName}
                    <button onClick={() => removeSkill(s.skillName)}><X className="h-3 w-3" /></button>
                  </Badge>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Experience</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {profile.experiences.map((exp, i) => (
                <div key={exp.id || i} className="flex items-start justify-between rounded-md border p-3">
                  <div>
                    <p className="font-medium">{exp.title}</p>
                    <p className="text-sm text-muted-foreground">{exp.company}</p>
                    {exp.startDate && <p className="text-xs text-muted-foreground">{exp.startDate} – {exp.endDate || "Present"}</p>}
                    {exp.description && <p className="mt-1 text-sm">{exp.description}</p>}
                  </div>
                  <Button size="sm" variant="ghost" onClick={() => removeExperience(i)}>
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>
              ))}
              <div className="border-t pt-3 space-y-2">
                <p className="text-xs font-medium text-muted-foreground">Add experience</p>
                <div className="grid grid-cols-2 gap-2">
                  <Input placeholder="Company" value={expForm.company} onChange={(e) => setExpForm({ ...expForm, company: e.target.value })} />
                  <Input placeholder="Title" value={expForm.title} onChange={(e) => setExpForm({ ...expForm, title: e.target.value })} />
                  <Input placeholder="Start date (YYYY-MM)" value={expForm.startDate} onChange={(e) => setExpForm({ ...expForm, startDate: e.target.value })} />
                  <Input placeholder="End date (YYYY-MM)" value={expForm.endDate} onChange={(e) => setExpForm({ ...expForm, endDate: e.target.value })} />
                </div>
                <Textarea placeholder="Description" rows={2} value={expForm.description} onChange={(e) => setExpForm({ ...expForm, description: e.target.value })} />
                <Button size="sm" onClick={saveExperience} disabled={!expForm.company || !expForm.title}>
                  <Plus className="mr-1 h-3 w-3" />Add experience
                </Button>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Projects</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {profile.projects.map((proj, i) => (
                <div key={proj.id || i} className="flex items-start justify-between rounded-md border p-3">
                  <div>
                    <p className="font-medium">{proj.name}</p>
                    {proj.description && <p className="text-sm">{proj.description}</p>}
                    {proj.technologies && (
                      <div className="mt-1 flex flex-wrap gap-1">
                        {proj.technologies.split(",").map((t) => (
                          <Badge key={t.trim()} variant="outline" className="text-xs">{t.trim()}</Badge>
                        ))}
                      </div>
                    )}
                    {proj.url && <a href={proj.url} target="_blank" rel="noopener noreferrer" className="text-xs text-primary hover:underline">{proj.url}</a>}
                  </div>
                  <Button size="sm" variant="ghost" onClick={() => removeProject(i)}>
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>
              ))}
              <div className="border-t pt-3 space-y-2">
                <p className="text-xs font-medium text-muted-foreground">Add project</p>
                <Input placeholder="Project name" value={projForm.name} onChange={(e) => setProjForm({ ...projForm, name: e.target.value })} />
                <Textarea placeholder="Description" rows={2} value={projForm.description} onChange={(e) => setProjForm({ ...projForm, description: e.target.value })} />
                <div className="grid grid-cols-2 gap-2">
                  <Input placeholder="Technologies (comma-separated)" value={projForm.technologies} onChange={(e) => setProjForm({ ...projForm, technologies: e.target.value })} />
                  <Input placeholder="URL" value={projForm.url} onChange={(e) => setProjForm({ ...projForm, url: e.target.value })} />
                </div>
                <Button size="sm" onClick={saveProject} disabled={!projForm.name}>
                  <Plus className="mr-1 h-3 w-3" />Add project
                </Button>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>CVs</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.doc,.docx,.txt"
                className="hidden"
                onChange={onFileChange}
              />

              <div
                onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={onDrop}
                onClick={() => fileInputRef.current?.click()}
                className={`flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed p-6 text-center transition-colors ${
                  dragOver
                    ? "border-primary bg-primary/5"
                    : "border-muted-foreground/25 hover:border-primary/50"
                }`}
              >
                {cvUploading ? (
                  <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                ) : (
                  <>
                    <Upload className="mb-2 h-8 w-8 text-muted-foreground" />
                    <p className="text-sm font-medium">
                      Drop your CV here or click to browse
                    </p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      PDF, DOC, DOCX, or TXT (max 10 MB)
                    </p>
                  </>
                )}
              </div>

              {cvs.length > 0 && (
                <div className="space-y-2 pt-2">
                  {cvs.map((cv) => (
                    <div key={cv.id} className="flex items-center justify-between rounded-md border p-3">
                      <div className="flex min-w-0 flex-1 items-center gap-2">
                        <FileText className="h-4 w-4 shrink-0 text-muted-foreground" />
                        <div className="min-w-0">
                          <p className="truncate font-medium">{cv.fileName}</p>
                          <p className="text-xs text-muted-foreground">
                            {cv.isDefault && <Badge variant="default" className="mr-2">Default</Badge>}
                            {new Date(cv.createdAt).toLocaleDateString()}
                          </p>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        {!cv.isDefault && (
                          <Button variant="outline" size="sm" onClick={() => setDefaultCv(cv.id)}>
                            Set default
                          </Button>
                        )}
                        <Button variant="destructive" size="sm" onClick={() => deleteCv(cv.id)}>
                          <X className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
              {cvs.length === 0 && (
                <p className="text-sm text-muted-foreground">No CVs uploaded yet.</p>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
