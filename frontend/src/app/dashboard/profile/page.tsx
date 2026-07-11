"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { parseSkills } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Briefcase, GraduationCap, Award, Link2, Plus, Trash2, Upload } from "lucide-react";

interface Profile {
  headline: string;
  summary: string;
  location: string;
  yearsOfExperience: number;
  primaryRole: string;
}

interface Skill { id: string; skillName: string; skillType: string; }
interface Experience { id: string; company: string; title: string; startDate: string; endDate: string; description: string; }
interface Project { id: string; name: string; description: string; technologies: string; url: string; }
interface Education { id: string; institution: string; degree: string; fieldOfStudy: string; startDate: string; endDate: string; description: string; }
interface Certification { id: string; name: string; issuingOrg: string; issueDate: string; expiryDate: string; credentialUrl: string; }
interface ProfileLink { id: string; linkType: string; url: string; label: string; }
interface CvDoc { id: string; fileName: string; isDefault: boolean; createdAt: string; }

export default function ProfilePage() {
  const [profile, setProfile] = useState<Profile>({ headline: "", summary: "", location: "", yearsOfExperience: 0, primaryRole: "" });
  const [skills, setSkills] = useState<Skill[]>([]);
  const [experience, setExperience] = useState<Experience[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [education, setEducation] = useState<Education[]>([]);
  const [certifications, setCertifications] = useState<Certification[]>([]);
  const [links, setLinks] = useState<ProfileLink[]>([]);
  const [cvDocs, setCvDocs] = useState<CvDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [newSkill, setNewSkill] = useState("");
  const [newExp, setNewExp] = useState<Partial<Experience>>({});
  const [newProject, setNewProject] = useState<Partial<Project>>({});
  const [newEdu, setNewEdu] = useState<Partial<Education>>({});
  const [newCert, setNewCert] = useState<Partial<Certification>>({});
  const [newLink, setNewLink] = useState<Partial<ProfileLink>>({});

  useEffect(() => {
    Promise.all([
      api.get<Profile>("/api/v1/profile").catch(() => null),
      api.get<{ skills: Skill[] }>("/api/v1/profile/skills").catch(() => ({ skills: [] })),
      api.get<{ experiences: Experience[] }>("/api/v1/profile/experience").catch(() => ({ experiences: [] })),
      api.get<{ projects: Project[] }>("/api/v1/profile/projects").catch(() => ({ projects: [] })),
      api.get<Education[]>("/api/v1/profile/education").catch(() => []),
      api.get<Certification[]>("/api/v1/profile/certifications").catch(() => []),
      api.get<ProfileLink[]>("/api/v1/profile/links").catch(() => []),
      api.get<{ cvDocuments: CvDoc[] }>("/api/v1/cvs").catch(() => ({ cvDocuments: [] })),
    ]).then(([p, s, e, pr, ed, ce, li, cv]) => {
      if (p) setProfile(p);
      setSkills(s?.skills || []);
      setExperience(e?.experiences || []);
      setProjects(pr?.projects || []);
      setEducation(Array.isArray(ed) ? ed : []);
      setCertifications(Array.isArray(ce) ? ce : []);
      setLinks(Array.isArray(li) ? li : []);
      setCvDocs(cv?.cvDocuments || []);
    }).finally(() => setLoading(false));
  }, []);

  const saveProfile = async () => {
    setSaving(true);
    try {
      await api.put("/api/v1/profile", profile);
      if (skills.length > 0) await api.put("/api/v1/profile/skills", { skills: skills.map((s) => ({ skillName: s.skillName, skillType: s.skillType })) });
      if (experience.length > 0) await api.put("/api/v1/profile/experience", { experience });
      if (projects.length > 0) await api.put("/api/v1/profile/projects", { projects });
      if (education.length > 0) await api.post("/api/v1/profile/education", { education });
      if (certifications.length > 0) await api.post("/api/v1/profile/certifications", { certifications });
      if (links.length > 0) await api.post("/api/v1/profile/links", { links });
    } catch {}
    setSaving(false);
  };

  const uploadCv = async (file: File) => {
    try {
      const result = await api.upload<{ id: string; fileName: string }>("/api/v1/cvs/upload", file);
      setCvDocs((prev) => [...prev, { id: result.id, fileName: result.fileName, isDefault: false, createdAt: new Date().toISOString() }]);
    } catch {}
  };

  const setDefaultCv = async (id: string) => {
    try {
      await api.put(`/api/v1/cvs/${id}/default`);
      setCvDocs((prev) => prev.map((cv) => ({ ...cv, isDefault: cv.id === id })));
    } catch {}
  };

  const deleteCv = async (id: string) => {
    try {
      await api.del(`/api/v1/cvs/${id}`);
      setCvDocs((prev) => prev.filter((cv) => cv.id !== id));
    } catch {}
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  const LINK_TYPES = ["github", "linkedin", "portfolio", "twitter", "other"];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Profile</h1>
        <Button onClick={saveProfile} disabled={saving}>{saving ? "Saving..." : "Save all"}</Button>
      </div>

      <Card>
        <CardHeader><CardTitle>CV Documents</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-3">
            {cvDocs.map((cv) => (
              <div key={cv.id} className="flex items-center gap-2 rounded-lg border p-3">
                <span className="text-sm font-medium">{cv.fileName}</span>
                {cv.isDefault && <Badge>Default</Badge>}
                {!cv.isDefault && <Button size="sm" variant="outline" onClick={() => setDefaultCv(cv.id)}>Set default</Button>}
                <Button size="sm" variant="ghost" onClick={() => deleteCv(cv.id)}><Trash2 className="h-4 w-4" /></Button>
              </div>
            ))}
          </div>
          <label className="flex cursor-pointer items-center gap-2 rounded-lg border border-dashed p-4 text-sm text-muted-foreground hover:bg-accent/50">
            <Upload className="h-4 w-4" />Upload CV (PDF/DOCX)
            <input type="file" className="hidden" accept=".pdf,.docx" onChange={(e) => e.target.files?.[0] && uploadCv(e.target.files[0])} />
          </label>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Basic Info</CardTitle></CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <div className="space-y-1"><label className="text-xs font-medium">Headline</label><Input value={profile.headline} onChange={(e) => setProfile({ ...profile, headline: e.target.value })} /></div>
          <div className="space-y-1"><label className="text-xs font-medium">Primary role</label><Input value={profile.primaryRole} onChange={(e) => setProfile({ ...profile, primaryRole: e.target.value })} /></div>
          <div className="space-y-1"><label className="text-xs font-medium">Location</label><Input value={profile.location} onChange={(e) => setProfile({ ...profile, location: e.target.value })} /></div>
          <div className="space-y-1"><label className="text-xs font-medium">Years of experience</label><Input type="number" value={profile.yearsOfExperience} onChange={(e) => setProfile({ ...profile, yearsOfExperience: parseInt(e.target.value) || 0 })} /></div>
          <div className="md:col-span-2 space-y-1"><label className="text-xs font-medium">Summary</label><textarea className="w-full rounded-md border bg-background px-3 py-2 text-sm min-h-[100px]" value={profile.summary} onChange={(e) => setProfile({ ...profile, summary: e.target.value })} /></div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2"><GraduationCap className="h-4 w-4" />Education</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {education.map((edu, i) => (
            <div key={edu.id || i} className="flex items-start gap-3 rounded-lg border p-3">
              <div className="flex-1">
                <p className="font-medium">{edu.degree} {edu.fieldOfStudy && `in ${edu.fieldOfStudy}`}</p>
                <p className="text-sm text-muted-foreground">{edu.institution}</p>
                <p className="text-xs text-muted-foreground">{edu.startDate} – {edu.endDate || "Present"}</p>
              </div>
              <Button size="sm" variant="ghost" onClick={() => setEducation(education.filter((_, j) => j !== i))}><Trash2 className="h-4 w-4" /></Button>
            </div>
          ))}
          <div className="flex flex-wrap gap-2">
            <Input placeholder="Institution" className="w-40" value={newEdu.institution || ""} onChange={(e) => setNewEdu({ ...newEdu, institution: e.target.value })} />
            <Input placeholder="Degree" className="w-32" value={newEdu.degree || ""} onChange={(e) => setNewEdu({ ...newEdu, degree: e.target.value })} />
            <Input placeholder="Field of study" className="w-36" value={newEdu.fieldOfStudy || ""} onChange={(e) => setNewEdu({ ...newEdu, fieldOfStudy: e.target.value })} />
            <Input type="date" className="w-36" value={newEdu.startDate || ""} onChange={(e) => setNewEdu({ ...newEdu, startDate: e.target.value })} />
            <Input type="date" className="w-36" value={newEdu.endDate || ""} onChange={(e) => setNewEdu({ ...newEdu, endDate: e.target.value })} />
            <Button size="sm" onClick={() => { if (newEdu.institution) { setEducation([...education, { id: "", ...newEdu } as Education]); setNewEdu({}); } }}><Plus className="mr-1 h-3 w-3" />Add</Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2"><Award className="h-4 w-4" />Certifications</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {certifications.map((cert, i) => (
            <div key={cert.id || i} className="flex items-start gap-3 rounded-lg border p-3">
              <div className="flex-1">
                <p className="font-medium">{cert.name}</p>
                <p className="text-sm text-muted-foreground">{cert.issuingOrg}</p>
                <p className="text-xs text-muted-foreground">{cert.issueDate} {cert.expiryDate && `– ${cert.expiryDate}`}</p>
              </div>
              <Button size="sm" variant="ghost" onClick={() => setCertifications(certifications.filter((_, j) => j !== i))}><Trash2 className="h-4 w-4" /></Button>
            </div>
          ))}
          <div className="flex flex-wrap gap-2">
            <Input placeholder="Certification name" className="w-48" value={newCert.name || ""} onChange={(e) => setNewCert({ ...newCert, name: e.target.value })} />
            <Input placeholder="Issuing org" className="w-40" value={newCert.issuingOrg || ""} onChange={(e) => setNewCert({ ...newCert, issuingOrg: e.target.value })} />
            <Input type="date" className="w-36" value={newCert.issueDate || ""} onChange={(e) => setNewCert({ ...newCert, issueDate: e.target.value })} />
            <Input placeholder="Credential URL" className="w-48" value={newCert.credentialUrl || ""} onChange={(e) => setNewCert({ ...newCert, credentialUrl: e.target.value })} />
            <Button size="sm" onClick={() => { if (newCert.name) { setCertifications([...certifications, { id: "", ...newCert } as Certification]); setNewCert({}); } }}><Plus className="mr-1 h-3 w-3" />Add</Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2"><Link2 className="h-4 w-4" />Links</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {links.map((link, i) => (
            <div key={link.id || i} className="flex items-center gap-3 rounded-lg border p-3">
              <Badge variant="outline">{link.linkType}</Badge>
              <span className="flex-1 text-sm truncate">{link.url}</span>
              {link.label && <span className="text-xs text-muted-foreground">{link.label}</span>}
              <Button size="sm" variant="ghost" onClick={() => setLinks(links.filter((_, j) => j !== i))}><Trash2 className="h-4 w-4" /></Button>
            </div>
          ))}
          <div className="flex flex-wrap gap-2">
            <select className="rounded-md border bg-background px-3 py-2 text-sm" value={newLink.linkType || ""} onChange={(e) => setNewLink({ ...newLink, linkType: e.target.value })}>
              <option value="">Type</option>
              {LINK_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
            <Input placeholder="URL" className="w-64" value={newLink.url || ""} onChange={(e) => setNewLink({ ...newLink, url: e.target.value })} />
            <Input placeholder="Label" className="w-32" value={newLink.label || ""} onChange={(e) => setNewLink({ ...newLink, label: e.target.value })} />
            <Button size="sm" onClick={() => { if (newLink.linkType && newLink.url) { setLinks([...links, { id: "", ...newLink } as ProfileLink]); setNewLink({}); } }}><Plus className="mr-1 h-3 w-3" />Add</Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2"><Briefcase className="h-4 w-4" />Skills</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2">
            {skills.map((s, i) => (
              <Badge key={i} variant="outline" className="cursor-pointer" onClick={() => setSkills(skills.filter((_, j) => j !== i))}>
                {s.skillName} <span className="ml-1 text-xs text-muted-foreground">x</span>
              </Badge>
            ))}
          </div>
          <div className="flex gap-2">
            <Input placeholder="Add skill" className="flex-1" value={newSkill} onChange={(e) => setNewSkill(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter" && newSkill.trim()) { setSkills([...skills, { id: "", skillName: newSkill.trim(), skillType: "technical" }]); setNewSkill(""); } }} />
            <Button onClick={() => { if (newSkill.trim()) { setSkills([...skills, { id: "", skillName: newSkill.trim(), skillType: "technical" }]); setNewSkill(""); } }}>Add</Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Experience</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {experience.map((exp, i) => (
            <div key={exp.id || i} className="rounded-lg border p-3">
              <div className="flex items-start justify-between">
                <div><p className="font-medium">{exp.title} at {exp.company}</p><p className="text-xs text-muted-foreground">{exp.startDate} – {exp.endDate || "Present"}</p></div>
                <Button size="sm" variant="ghost" onClick={() => setExperience(experience.filter((_, j) => j !== i))}><Trash2 className="h-4 w-4" /></Button>
              </div>
            </div>
          ))}
          <div className="flex flex-wrap gap-2">
            <Input placeholder="Title" className="w-36" value={newExp.title || ""} onChange={(e) => setNewExp({ ...newExp, title: e.target.value })} />
            <Input placeholder="Company" className="w-36" value={newExp.company || ""} onChange={(e) => setNewExp({ ...newExp, company: e.target.value })} />
            <Input type="date" className="w-36" value={newExp.startDate || ""} onChange={(e) => setNewExp({ ...newExp, startDate: e.target.value })} />
            <Input type="date" className="w-36" value={newExp.endDate || ""} onChange={(e) => setNewExp({ ...newExp, endDate: e.target.value })} />
            <Button size="sm" onClick={() => { if (newExp.title && newExp.company) { setExperience([...experience, { id: "", description: "", ...newExp } as Experience]); setNewExp({}); } }}><Plus className="mr-1 h-3 w-3" />Add</Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Projects</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {projects.map((proj, i) => (
            <div key={proj.id || i} className="flex items-start justify-between rounded-lg border p-3">
              <div><p className="font-medium">{proj.name}</p><p className="text-sm text-muted-foreground">{proj.technologies}</p></div>
              <Button size="sm" variant="ghost" onClick={() => setProjects(projects.filter((_, j) => j !== i))}><Trash2 className="h-4 w-4" /></Button>
            </div>
          ))}
          <div className="flex flex-wrap gap-2">
            <Input placeholder="Project name" className="w-40" value={newProject.name || ""} onChange={(e) => setNewProject({ ...newProject, name: e.target.value })} />
            <Input placeholder="Technologies" className="w-40" value={newProject.technologies || ""} onChange={(e) => setNewProject({ ...newProject, technologies: e.target.value })} />
            <Input placeholder="URL" className="w-48" value={newProject.url || ""} onChange={(e) => setNewProject({ ...newProject, url: e.target.value })} />
            <Button size="sm" onClick={() => { if (newProject.name) { setProjects([...projects, { id: "", description: "", ...newProject } as Project]); setNewProject({}); } }}><Plus className="mr-1 h-3 w-3" />Add</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
