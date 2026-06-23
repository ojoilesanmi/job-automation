"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Plus, X } from "lucide-react";

interface Preferences {
  id: string;
  targetRoles: string | null;
  preferredSkills: string | null;
  remoteFirst: boolean;
  relocationFriendly: boolean;
  preferredCountries: string | null;
  excludedCountries: string | null;
  excludedCompanies: string | null;
  remoteMinSalary: number | null;
  maxApplicationsPerDay: number;
  approvalRequired: boolean;
}

function parseList(val: string | null): string[] {
  if (!val) return [];
  return val.split(",").map((s) => s.trim()).filter(Boolean);
}

function toList(arr: string[]): string | null {
  return arr.length > 0 ? arr.join(", ") : null;
}

export default function PreferencesPage() {
  const [prefs, setPrefs] = useState<Preferences>({
    id: "", targetRoles: null, preferredSkills: null, remoteFirst: true,
    relocationFriendly: false, preferredCountries: null, excludedCountries: null,
    excludedCompanies: null, remoteMinSalary: null, maxApplicationsPerDay: 10, approvalRequired: true,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [roles, setRoles] = useState<string[]>([]);
  const [skills, setSkills] = useState<string[]>([]);
  const [countries, setCountries] = useState<string[]>([]);
  const [newRole, setNewRole] = useState("");
  const [newSkill, setNewSkill] = useState("");
  const [newCountry, setNewCountry] = useState("");

  useEffect(() => {
    api.get<Preferences>("/api/v1/preferences").then((p) => {
      setPrefs(p);
      setRoles(parseList(p.targetRoles));
      setSkills(parseList(p.preferredSkills));
      setCountries(parseList(p.preferredCountries));
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const save = async () => {
    setSaving(true);
    try {
      const updated = await api.put<Preferences>("/api/v1/preferences", {
        ...prefs,
        targetRoles: toList(roles),
        preferredSkills: toList(skills),
        preferredCountries: toList(countries),
      });
      setPrefs(updated);
    } catch {}
    setSaving(false);
  };

  const addToList = (arr: string[], setArr: (v: string[]) => void, val: string, setVal: (v: string) => void) => {
    const trimmed = val.trim();
    if (trimmed && !arr.includes(trimmed)) {
      setArr([...arr, trimmed]);
      setVal("");
    }
  };

  const removeFromList = (arr: string[], setArr: (v: string[]) => void, item: string) => {
    setArr(arr.filter((x) => x !== item));
  };

  const ListInput = ({ label, items, setItems, value, setValue }: {
    label: string; items: string[]; setItems: (v: string[]) => void; value: string; setValue: (v: string) => void;
  }) => (
    <div className="space-y-2">
      <Label>{label}</Label>
      <div className="flex gap-2">
        <Input placeholder={`Add ${label.toLowerCase()}`} value={value} onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && addToList(items, setItems, value, setValue)} />
        <Button size="icon" onClick={() => addToList(items, setItems, value, setValue)}><Plus className="h-4 w-4" /></Button>
      </div>
      <div className="flex flex-wrap gap-2">
        {items.map((item) => (
          <Badge key={item} variant="secondary" className="gap-1">
            {item}
            <button onClick={() => removeFromList(items, setItems, item)}><X className="h-3 w-3" /></button>
          </Badge>
        ))}
      </div>
    </div>
  );

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Preferences</h1>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Job preferences</CardTitle></CardHeader>
          <CardContent className="space-y-6">
            <ListInput label="Target roles" items={roles} setItems={setRoles} value={newRole} setValue={setNewRole} />
            <ListInput label="Preferred countries" items={countries} setItems={setCountries} value={newCountry} setValue={setNewCountry} />
            <ListInput label="Skills" items={skills} setItems={setSkills} value={newSkill} setValue={setNewSkill} />
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Remote minimum salary</Label>
                <Input type="number" value={prefs.remoteMinSalary ?? ""} onChange={(e) => setPrefs({ ...prefs, remoteMinSalary: e.target.value ? +e.target.value : null })} />
              </div>
              <div className="space-y-2">
                <Label>Max applications/day</Label>
                <Input type="number" value={prefs.maxApplicationsPerDay} onChange={(e) => setPrefs({ ...prefs, maxApplicationsPerDay: +e.target.value })} />
              </div>
            </div>
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <input type="checkbox" id="remote" checked={prefs.remoteFirst} onChange={(e) => setPrefs({ ...prefs, remoteFirst: e.target.checked })} className="h-4 w-4" />
                <Label htmlFor="remote">Remote first</Label>
              </div>
              <div className="flex items-center gap-2">
                <input type="checkbox" id="relocation" checked={prefs.relocationFriendly} onChange={(e) => setPrefs({ ...prefs, relocationFriendly: e.target.checked })} className="h-4 w-4" />
                <Label htmlFor="relocation">Open to relocation</Label>
              </div>
              <div className="flex items-center gap-2">
                <input type="checkbox" id="approval" checked={prefs.approvalRequired} onChange={(e) => setPrefs({ ...prefs, approvalRequired: e.target.checked })} className="h-4 w-4" />
                <Label htmlFor="approval">Require approval before applying</Label>
              </div>
            </div>
            <Button onClick={save} disabled={saving}>{saving ? "Saving..." : "Save preferences"}</Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
