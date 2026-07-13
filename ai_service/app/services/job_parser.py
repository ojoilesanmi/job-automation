from __future__ import annotations
import structlog
import re
import bleach
from ..exceptions import ValidationError
from ..constants import TECH_SKILLS

logger = structlog.get_logger()

REMOTE_KEYWORDS = {
    "full_remote": ["fully remote", "100% remote", "remote only", "work from anywhere"],
    "remote": ["remote", "work from home", "wfh", "distributed", "remote-first"],
    "hybrid": ["hybrid", "flexible location", "remote/on-site"],
    "onsite": ["on-site", "onsite", "in office", "in-office"],
}

RELOCATION_KEYWORDS = ["relocation", "relocation assistance", "relocation support", "relocation package"]
VISA_KEYWORDS = ["visa sponsorship", "visa sponsor", "h1b", "h-1b", "work visa", "sponsor visa"]


def parse_job_description(request: dict) -> dict:
    description = request.get("description", "")
    title = request.get("title", "")
    company = request.get("company", "")

    if not description or len(description.strip()) < 20:
        raise ValidationError("Job description is too short or empty")

    logger.info("parsing_job", title=title, company=company, desc_length=len(description))

    cleaned = bleach.clean(description, tags=[], strip=True)
    cleaned = re.sub(r'\s+', ' ', cleaned).strip()

    salary_min, salary_max, currency = _extract_salary(cleaned)

    return {
        "cleanedDescription": cleaned[:5000],
        "requiredSkills": _extract_skills(cleaned),
        "preferredSkills": _extract_preferred_skills(cleaned),
        "remoteType": _detect_remote_type(cleaned),
        "relocationAvailable": any(kw.lower() in cleaned.lower() for kw in RELOCATION_KEYWORDS),
        "visaSponsorship": any(kw.lower() in cleaned.lower() for kw in VISA_KEYWORDS),
        "seniority": _detect_seniority(title, cleaned),
        "experienceYears": _extract_experience_years(cleaned),
        "salaryMin": salary_min,
        "salaryMax": salary_max,
        "currency": currency,
        "employmentType": _detect_employment_type(cleaned),
    }


def _extract_skills(text: str) -> list[str]:
    text_lower = text.lower()
    section = re.search(
        r'(?:required|must have|requirements|qualifications)[:\s]*(.*?)(?:\n\s*(?:preferred|nice to have|bonus|about))',
        text_lower, re.DOTALL,
    )
    search = section.group(1) if section else text_lower
    return list(dict.fromkeys(s for s in TECH_SKILLS if s in search))


def _extract_preferred_skills(text: str) -> list[str]:
    match = re.search(r'(?:preferred|nice to have|bonus|plus)[:\s]*(.*?)(?:\n\s*\n|\n[A-Z])', text.lower(), re.DOTALL)
    if not match:
        return []
    return list(dict.fromkeys(s for s in TECH_SKILLS if s in match.group(1)))


def _detect_remote_type(text: str) -> str:
    lower = text.lower()
    for rtype, keywords in REMOTE_KEYWORDS.items():
        if any(kw in lower for kw in keywords):
            return rtype
    return "onsite"


def _detect_seniority(title: str, desc: str) -> str:
    combined = f"{title} {desc}".lower()
    if any(k in combined for k in ['staff', 'principal', 'distinguished', 'fellow']):
        return "staff"
    if any(k in combined for k in ['senior', 'sr.', 'lead', 'head']):
        return "senior"
    if any(k in combined for k in ['junior', 'jr.', 'entry level', 'associate', 'intern']):
        return "junior"
    return "mid"


def _extract_experience_years(text: str) -> int | None:
    m = re.search(r'(\d+)\+?\s*(?:years?|yrs?)\s*(?:of)?\s*experience', text.lower())
    return int(m.group(1)) if m else None


def _extract_salary(text: str) -> tuple:
    m = re.search(r'[\$€£]?\s*(\d{1,3}(?:,\d{3})*(?:k)?)\s*[-–to]+\s*[\$€£]?\s*(\d{1,3}(?:,\d{3})*(?:k)?)', text)
    if not m:
        return None, None, None

    salary_min = _parse_num(m.group(1))
    salary_max = _parse_num(m.group(2))

    salary_text = m.group(0)
    prefix = text[:m.start()]
    if '€' in salary_text or '€' in prefix:
        currency = "EUR"
    elif '£' in salary_text or '£' in prefix:
        currency = "GBP"
    else:
        currency = "USD"

    return salary_min, salary_max, currency


def _parse_num(s: str) -> float:
    s = s.replace(',', '').replace('$', '').replace('€', '').replace('£', '')
    return float(s[:-1]) * 1000 if s.lower().endswith('k') else float(s)


def _detect_employment_type(text: str) -> str:
    lower = text.lower()
    if any(k in lower for k in ['full-time', 'fulltime', 'full time']):
        return "full_time"
    if any(k in lower for k in ['part-time', 'parttime', 'part time']):
        return "part_time"
    if any(k in lower for k in ['contract', 'contractor', 'freelance']):
        return "contract"
    if any(k in lower for k in ['internship', 'intern']):
        return "internship"
    return "full_time"
