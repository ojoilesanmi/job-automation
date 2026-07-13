import type {
  User,
  Profile,
  Job,
  JobMatch,
  CoverLetter,
  Application,
  Preferences,
  DashboardStats,
  PaginatedResponse,
  JobDetail,
  JobMatchDetail,
  CoverLetterResponse,
  CoverLetterTemplate,
  SkillGap,
  ExperienceGap,
  GapAnalysis,
  DetectedQuestion,
  CompanyResearch,
} from "./index";

describe("Shared types", () => {
  it("User type has required fields", () => {
    const user: User = {
      id: "1",
      email: "test@example.com",
      firstName: "John",
      lastName: "Doe",
      roles: ["USER"],
      permissions: [],
    };
    expect(user.id).toBe("1");
    expect(user.email).toContain("@");
  });

  it("Job type has required fields", () => {
    const job: Job = {
      id: "1",
      title: "Software Engineer",
      company: "Acme",
      description: "A great job",
      skills: ["java"],
      scrapedAt: "2026-01-01",
    };
    expect(job.title).toBeTruthy();
    expect(job.skills).toBeInstanceOf(Array);
  });

  it("JobMatch type has scoring fields", () => {
    const match: JobMatch = {
      id: "1",
      jobId: "1",
      profileId: "1",
      matchScore: 85,
      skillMatchScore: 90,
      experienceMatchScore: 80,
      locationMatchScore: 70,
      educationMatchScore: 75,
      missingSkills: [],
      status: "pending",
    };
    expect(match.matchScore).toBeGreaterThan(0);
  });

  it("PaginatedResponse wraps content", () => {
    const response: PaginatedResponse<Job> = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 10,
    };
    expect(response.content).toBeInstanceOf(Array);
    expect(response.size).toBe(10);
  });

  it("CoverLetterResponse has versioning", () => {
    const cl: CoverLetterResponse = {
      id: "1",
      jobId: "1",
      jobTitle: "Engineer",
      company: "Acme",
      content: "Dear Hiring Manager...",
      version: 1,
      status: "draft",
      createdAt: "2026-01-01",
      updatedAt: "2026-01-01",
    };
    expect(cl.version).toBe(1);
  });

  it("GapAnalysis structures skill and experience gaps", () => {
    const analysis: GapAnalysis = {
      missingSkills: [{ skill: "Kubernetes", severity: "critical", recommendation: "Learn K8s" }],
      experienceGaps: [{ area: "Leadership", description: "No lead exp", suggestion: "Take a lead role" }],
      summary: "Some gaps found",
    };
    expect(analysis.missingSkills).toHaveLength(1);
    expect(analysis.experienceGaps[0].area).toBe("Leadership");
  });

  it("CompanyResearch has all research fields", () => {
    const research: CompanyResearch = {
      companyName: "Acme",
      summary: "A tech company",
      companySize: "100-500",
      industry: "Technology",
      techStack: "Java, Python",
      glassdoorRating: "4.2",
      linkedInUrl: "https://linkedin.com/company/acme",
    };
    expect(research.companyName).toBe("Acme");
    expect(research.techStack).toContain("Java");
  });

  it("DetectedQuestion tracks answer status", () => {
    const q: DetectedQuestion = {
      id: "1",
      text: "Why do you want this role?",
      type: "text",
      required: true,
      answered: false,
    };
    expect(q.required).toBe(true);
    expect(q.answered).toBe(false);
  });
});
