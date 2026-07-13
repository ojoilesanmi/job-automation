from __future__ import annotations
from pydantic import BaseModel, Field
from typing import Optional


class BaseResponse(BaseModel):
    response_code: str = Field(alias="responseCode")
    response_status: str = Field(alias="responseStatus")
    response_message: str = Field(alias="responseMessage")
    data: Optional[dict] = None

    class Config:
        populate_by_name = True


# --- CV Parsing ---

class CvParseRequest(BaseModel):
    file_url: Optional[str] = Field(None, alias="fileUrl")
    file_content_base64: Optional[str] = Field(None, alias="fileContentBase64")
    file_type: str = Field(..., alias="fileType", pattern="^(pdf|docx)$")

    class Config:
        populate_by_name = True


class CvParseResponse(BaseModel):
    full_name: Optional[str] = Field(None, alias="fullName")
    raw_text: Optional[str] = Field(None, alias="rawText")
    email: Optional[str] = None
    phone: Optional[str] = None
    location: Optional[str] = None
    summary: Optional[str] = None
    skills: list[str] = []
    work_experience: list[dict] = Field(default_factory=list, alias="workExperience")
    projects: list[dict] = []
    education: list[dict] = []
    certifications: list[str] = []
    links: list[str] = []

    class Config:
        populate_by_name = True


# --- Job Parsing ---

class JobParseRequest(BaseModel):
    title: Optional[str] = None
    company: Optional[str] = None
    description: str
    url: Optional[str] = None


class JobParseResponse(BaseModel):
    cleaned_description: str = Field(alias="cleanedDescription")
    required_skills: list[str] = Field(default_factory=list, alias="requiredSkills")
    preferred_skills: list[str] = Field(default_factory=list, alias="preferredSkills")
    seniority: Optional[str] = None
    experience_years: Optional[int] = Field(None, alias="experienceYears")
    remote_type: Optional[str] = Field(None, alias="remoteType")
    relocation_available: Optional[bool] = Field(None, alias="relocationAvailable")
    visa_sponsorship: Optional[bool] = Field(None, alias="visaSponsorship")
    salary_min: Optional[float] = Field(None, alias="salaryMin")
    salary_max: Optional[float] = Field(None, alias="salaryMax")
    currency: Optional[str] = None
    employment_type: Optional[str] = Field(None, alias="employmentType")

    class Config:
        populate_by_name = True


# --- Fit Analysis ---

class FitAnalysisRequest(BaseModel):
    user_profile: dict = Field(..., alias="userProfile")
    user_skills: list[str] = Field(default_factory=list, alias="userSkills")
    job_description: str = Field(..., alias="jobDescription")
    job_required_skills: list[str] = Field(default_factory=list, alias="jobRequiredSkills")
    job_preferred_skills: list[str] = Field(default_factory=list, alias="jobPreferredSkills")
    job_title: str = Field(..., alias="jobTitle")
    job_company: str = Field(..., alias="jobCompany")

    class Config:
        populate_by_name = True


class FitAnalysisResponse(BaseModel):
    fit_score: float = Field(..., alias="fitScore")
    matched_skills: list[str] = Field(default_factory=list, alias="matchedSkills")
    missing_skills: list[str] = Field(default_factory=list, alias="missingSkills")
    reasons_to_apply: list[str] = Field(default_factory=list, alias="reasonsToApply")
    reasons_to_skip: list[str] = Field(default_factory=list, alias="reasonsToSkip")
    risk_flags: list[str] = Field(default_factory=list, alias="riskFlags")
    explanation: str

    class Config:
        populate_by_name = True


# --- Cover Letter ---

class CoverLetterRequest(BaseModel):
    user_profile: dict = Field(..., alias="userProfile")
    cv_text: str = Field(..., alias="cvText")
    job_title: str = Field(..., alias="jobTitle")
    job_company: str = Field(..., alias="jobCompany")
    job_description: str = Field(..., alias="jobDescription")
    matched_skills: list[str] = Field(default_factory=list, alias="matchedSkills")
    missing_skills: list[str] = Field(default_factory=list, alias="missingSkills")
    tone: str = "professional"
    max_length: int = Field(500, alias="maxLength")

    class Config:
        populate_by_name = True


class CoverLetterResponse(BaseModel):
    content: str
    job_specific_points: list[str] = Field(default_factory=list, alias="jobSpecificPoints")
    unsupported_claims: list[str] = Field(default_factory=list, alias="unsupportedClaims")
    confidence_score: float = Field(..., alias="confidenceScore")

    class Config:
        populate_by_name = True

    @property
    def confidenceScore(self) -> float:
        return self.confidence_score


# --- Injection Check ---

class InjectionCheckRequest(BaseModel):
    text: str


class InjectionCheckResponse(BaseModel):
    is_safe: bool = Field(..., alias="isSafe")
    risk_level: str = Field(..., alias="riskLevel")
    flagged_patterns: list[str] = Field(default_factory=list, alias="flaggedPatterns")
    sanitized_text: Optional[str] = Field(None, alias="sanitizedText")

    class Config:
        populate_by_name = True


# --- Text Quality ---

class TextQualityRequest(BaseModel):
    text: str
    context: str = "cover_letter"


class TextQualityResponse(BaseModel):
    overall_score: float = Field(..., alias="overallScore")
    issues: list[dict] = []
    suggestions: list[str] = []
    word_count: int = Field(..., alias="wordCount")
    reading_level: str = Field(..., alias="readingLevel")

    class Config:
        populate_by_name = True
