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
  targetSeniority: string | null;
  preferredSkills: string | null;
  mustHaveSkills: string | null;
  niceToHaveSkills: string | null;
  remoteFirst: boolean;
  relocationFriendly: boolean;
  preferredCountries: string | null;
  excludedCountries: string | null;
  excludedCompanies: string | null;
  remoteMinSalary: number | null;
  relocationMinSalary: number | null;
  nigeriaMinSalary: number | null;
  minimumRemoteFitScore: number | null;
  minimumRelocationFitScore: number | null;
  minimumNigeriaFitScore: number | null;
  maxApplicationsPerDay: number;
  approvalRequired: boolean;
  autoRejectRules: string | null;
  excludedJobLevels: string | null;
  excludedIndustries: string | null;
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
    id: "", targetRoles: null, targetSeniority: null, preferredSkills: null,
    mustHaveSkills: null, niceToHaveSkills: null,
    remoteFirst: true, relocationFriendly: false,
    preferredCountries: null, excludedCountries: null, excludedCompanies: null,
    remoteMinSalary: null, relocationMinSalary: null, nigeriaMinSalary: null,
    minimumRemoteFitScore: null, minimumRelocationFitScore: null, minimumNigeriaFitScore: null,
    maxApplicationsPerDay: 10, approvalRequired: true,
    autoRejectRules: null, excludedJobLevels: null, excludedIndustries: null,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [roles, setRoles] = useState<string[]>([]);
  const [skills, setSkills] = useState<string[]>([]);
  const [countries, setCountries] = useState<string[]>([]);
  const [excludedCountriesList, setExcludedCountriesList] = useState<string[]>([]);
  const [excludedCompaniesList, setExcludedCompaniesList] = useState<string[]>([]);
  const [excludedJobLevelsList, setExcludedJobLevelsList] = useState<string[]>([]);
  const [excludedIndustriesList, setExcludedIndustriesList] = useState<string[]>([]);
  const [mustHaveSkillsList, setMustHaveSkillsList] = useState<string[]>([]);
  const [niceToHaveSkillsList, setNiceToHaveSkillsList] = useState<string[]>([]);
  const [newRole, setNewRole] = useState("");
  const [newSkill, setNewSkill] = useState("");
  const [newCountry, setNewCountry] = useState("");
  const [newExcludedCountry, setNewExcludedCountry] = useState("");
  const [newExcludedCompany, setNewExcludedCompany] = useState("");
  const [newExcludedLevel, setNewExcludedLevel] = useState("");
  const [newExcludedIndustry, setNewExcludedIndustry] = useState("");
  const [newMustHave, setNewMustHave] = useState("");
  const [newNiceToHave, setNewNiceToHave] = useState("");

  useEffect(() => {
    api.get<Preferences>("/api/v1/preferences").then((p) => {
      setPrefs(p);
      setRoles(parseList(p.targetRoles));
      setSkills(parseList(p.preferredSkills));
      setCountries(parseList(p.preferredCountries));
      setExcludedCountriesList(parseList(p.excludedCountries));
      setExcludedCompaniesList(parseList(p.excludedCompanies));
      setExcludedJobLevelsList(parseList(p.excludedJobLevels));
      setExcludedIndustriesList(parseList(p.excludedIndustries));
      setMustHaveSkillsList(parseList(p.mustHaveSkills));
      setNiceToHaveSkillsList(parseList(p.niceToHaveSkills));
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
        excludedCountries: toList(excludedCountriesList),
        excludedCompanies: toList(excludedCompaniesList),
        excludedJobLevels: toList(excludedJobLevelsList),
        excludedIndustries: toList(excludedIndustriesList),
        mustHaveSkills: toList(mustHaveSkillsList),
        niceToHaveSkills: toList(niceToHaveSkillsList),
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
          <CardHeader><CardTitle>Target roles & skills</CardTitle></CardHeader>
          <CardContent className="space-y-6">
            <ListInput label="Target roles" items={roles} setItems={setRoles} value={newRole} setValue={setNewRole} />
            <div className="space-y-2">
              <Label>Target seniority</Label>
              <Input value={prefs.targetSeniority ?? ""} onChange={(e) => setPrefs({ ...prefs, targetSeniority: e.target.value || null })} placeholder="e.g. Senior, Staff, Lead" />
            </div>
            <ListInput label="Preferred skills" items={skills} setItems={setSkills} value={newSkill} setValue={setNewSkill} />
            <ListInput label="Must-have skills" items={mustHaveSkillsList} setItems={setMustHaveSkillsList} value={newMustHave} setValue={setNewMustHave} />
            <ListInput label="Nice-to-have skills" items={niceToHaveSkillsList} setItems={setNiceToHaveSkillsList} value={newNiceToHave} setValue={setNewNiceToHave} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Location & remote</CardTitle></CardHeader>
          <CardContent className="space-y-6">
            <ListInput label="Preferred countries" items={countries} setItems={setCountries} value={newCountry} setValue={setNewCountry} />
            <ListInput label="Excluded countries" items={excludedCountriesList} setItems={setExcludedCountriesList} value={newExcludedCountry} setValue={setNewExcludedCountry} />
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <input type="checkbox" id="remote" checked={prefs.remoteFirst} onChange={(e) => setPrefs({ ...prefs, remoteFirst: e.target.checked })} className="h-4 w-4" />
                <Label htmlFor="remote">Remote first</Label>
              </div>
              <div className="flex items-center gap-2">
                <input type="checkbox" id="relocation" checked={prefs.relocationFriendly} onChange={(e) => setPrefs({ ...prefs, relocationFriendly: e.target.checked })} className="h-4 w-4" />
                <Label htmlFor="relocation">Open to relocation</Label>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Salary thresholds</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label>Remote minimum salary</Label>
              <Input type="number" value={prefs.remoteMinSalary ?? ""} onChange={(e) => setPrefs({ ...prefs, remoteMinSalary: e.target.value ? +e.target.value : null })} />
            </div>
            <div className="space-y-2">
              <Label>Relocation minimum salary</Label>
              <Input type="number" value={prefs.relocationMinSalary ?? ""} onChange={(e) => setPrefs({ ...prefs, relocationMinSalary: e.target.value ? +e.target.value : null })} />
            </div>
            <div className="space-y-2">
              <Label>Nigeria minimum salary</Label>
              <Input type="number" value={prefs.nigeriaMinSalary ?? ""} onChange={(e) => setPrefs({ ...prefs, nigeriaMinSalary: e.target.value ? +e.target.value : null })} />
            </div>
            <div className="space-y-2">
              <Label>Minimum remote fit score</Label>
              <Input type="number" value={prefs.minimumRemoteFitScore ?? ""} onChange={(e) => setPrefs({ ...prefs, minimumRemoteFitScore: e.target.value ? +e.target.value : null })} placeholder="Default: 75" />
            </div>
            <div className="space-y-2">
              <Label>Minimum relocation fit score</Label>
              <Input type="number" value={prefs.minimumRelocationFitScore ?? ""} onChange={(e) => setPrefs({ ...prefs, minimumRelocationFitScore: e.target.value ? +e.target.value : null })} placeholder="Default: 70" />
            </div>
            <div className="space-y-2">
              <Label>Minimum Nigeria fit score</Label>
              <Input type="number" value={prefs.minimumNigeriaFitScore ?? ""} onChange={(e) => setPrefs({ ...prefs, minimumNigeriaFitScore: e.target.value ? +e.target.value : null })} placeholder="Default: 85" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Exclusions & rules</CardTitle></CardHeader>
          <CardContent className="space-y-6">
            <ListInput label="Excluded companies" items={excludedCompaniesList} setItems={setExcludedCompaniesList} value={newExcludedCompany} setValue={setNewExcludedCompany} />
            <ListInput label="Excluded job levels" items={excludedJobLevelsList} setItems={setExcludedJobLevelsList} value={newExcludedLevel} setValue={setNewExcludedLevel} />
            <ListInput label="Excluded industries" items={excludedIndustriesList} setItems={setExcludedIndustriesList} value={newExcludedIndustry} setValue={setNewExcludedIndustry} />
            <div className="space-y-2">
              <Label>Max applications per day</Label>
              <Input type="number" value={prefs.maxApplicationsPerDay} onChange={(e) => setPrefs({ ...prefs, maxApplicationsPerDay: +e.target.value })} />
            </div>
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <input type="checkbox" id="approval" checked={prefs.approvalRequired} onChange={(e) => setPrefs({ ...prefs, approvalRequired: e.target.checked })} className="h-4 w-4" />
                <Label htmlFor="approval">Require approval before applying</Label>
              </div>
            </div>
            <div className="space-y-2">
              <Label>Auto-reject rules</Label>
              <Input value={prefs.autoRejectRules ?? ""} onChange={(e) => setPrefs({ ...prefs, autoRejectRules: e.target.value || null })} placeholder="e.g. salary below 50000, title contains intern" />
            </div>
          </CardContent>
        </Card>
      </div>

      <Button onClick={save} disabled={saving} className="w-full">{saving ? "Saving..." : "Save preferences"}</Button>
    </div>
  );
}
