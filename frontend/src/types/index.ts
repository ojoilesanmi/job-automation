export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  permissions: string[];
}

export interface Profile {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  phone?: string;
  location?: string;
  summary?: string;
  yearsExperience?: number;
  skills: string[];
}

export interface Cv {
  id: string;
  userId: string;
  profileId?: string;
  fileName: string;
  storageUri: string;
  fileHash: string;
  fileType: string;
  parsedText?: string;
  createdAt: string;
}

export interface Experience {
  id: string;
  profileId: string;
  company: string;
  title: string;
  startDate: string;
  endDate?: string;
  isCurrent: boolean;
  description: string;
}

export interface Education {
  id: string;
  profileId: string;
  institution: string;
  degree: string;
  fieldOfStudy: string;
  startDate: string;
  endDate?: string;
}

export interface Job {
  id: string;
  title: string;
  company: string;
  location?: string;
  description: string;
  source?: string;
  url?: string;
  salaryMin?: number;
  salaryMax?: number;
  employmentType?: string;
  experienceLevel?: string;
  skills: string[];
  remote?: boolean;
  scrapedAt: string;
}

export interface JobMatch {
  id: string;
  jobId: string;
  profileId: string;
  matchScore: number;
  skillMatchScore: number;
  experienceMatchScore: number;
  locationMatchScore: number;
  educationMatchScore: number;
  missingSkills: string[];
  matchReasoning?: string;
  status: "pending" | "approved" | "rejected" | "applied";
  job?: Job;
  coverLetter?: CoverLetter;
}

export interface CoverLetter {
  id: string;
  jobId: string;
  profileId: string;
  title: string;
  body: string;
  tone: string;
  language?: string;
  version: number;
  isApproved: boolean;
  qualityScore?: number;
  qualityIssues?: Record<string, unknown>;
  createdAt: string;
}

export interface Application {
  id: string;
  jobId: string;
  userId: string;
  coverLetterId?: string;
  matchId?: string;
  status: "draft" | "pending_approval" | "approved" | "submitted" | "rejected";
  notes?: string;
  job?: Job;
  coverLetter?: CoverLetter;
}

export interface Preferences {
  roles: string[];
  locations: string[];
  skills: string[];
  minSalary?: number;
  maxHoursPerWeek?: number;
  employmentTypes: string[];
  experienceLevel?: string;
  remote?: boolean;
}

export interface DashboardStats {
  totalJobs: number;
  pendingApprovals: number;
  appliedThisWeek: number;
  averageMatchScore: number;
  recentMatches: JobMatch[];
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
