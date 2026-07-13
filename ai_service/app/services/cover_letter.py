from __future__ import annotations
import structlog
import re
from openai import AsyncOpenAI, APITimeoutError, APIStatusError, APIConnectionError
from ..config.settings import settings
from ..models.schemas import CoverLetterRequest, CoverLetterResponse
from ..exceptions import UpstreamFailureError, ServiceUnavailableError

logger = structlog.get_logger()

client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY) if settings.OPENAI_API_KEY else None

TONE_INSTRUCTIONS = {
    "professional": "Write in a formal, professional tone. Be confident but not arrogant.",
    "enthusiastic": "Write with genuine enthusiasm and energy. Show excitement about the role.",
    "concise": "Write brief, punchy sentences. Get to the point quickly. No filler.",
    "storytelling": "Open with a brief anecdote. Make it personal and engaging.",
    "technical": "Focus on technical depth. Highlight specific technologies and decisions.",
}


async def generate_cover_letter(request: CoverLetterRequest) -> CoverLetterResponse:
    logger.info("cover_letter_start", job_title=request.job_title, company=request.job_company)

    from app.services.injection_guard import check_injection
    from app.models.schemas import InjectionCheckRequest
    guard_result = check_injection(InjectionCheckRequest(text=request.job_description))
    if not guard_result.is_safe:
        logger.warning(f"Injection detected in job description: {guard_result.flagged_patterns}")
        safe_description = guard_result.sanitized_text
    else:
        safe_description = request.job_description

    if not client:
        logger.info("cover_letter_fallback_template")
        return _template_cover_letter(request)

    try:
        response = await client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {"role": "system", "content": _system_prompt(request.tone)},
                {"role": "user", "content": _build_prompt(request, safe_description)},
            ],
            temperature=settings.OPENAI_TEMPERATURE,
            max_tokens=settings.OPENAI_MAX_TOKENS,
        )

        content = response.choices[0].message.content
        if not content:
            logger.warning("cover_letter_empty_response")
            return _template_cover_letter(request)

        content = content.strip()
        unsupported = _check_unsupported(content, request)
        confidence = max(0.0, 100.0 - len(unsupported) * 20 - (0 if request.matched_skills else 15))

        logger.info("cover_letter_complete", word_count=len(content.split()))
        return CoverLetterResponse(
            content=content,
            jobSpecificPoints=_job_points(content, request),
            unsupportedClaims=unsupported,
            confidenceScore=round(confidence, 1),
        )

    except APITimeoutError:
        logger.warning("cover_letter_timeout_fallback")
        return _template_cover_letter(request)
    except APIConnectionError:
        raise ServiceUnavailableError("Cannot connect to OpenAI")
    except APIStatusError as e:
        if e.status_code == 429:
            raise UpstreamFailureError("openai", "OpenAI rate limit exceeded")
        if e.status_code >= 500:
            logger.warning("cover_letter_upstream_error", status=e.status_code)
            return _template_cover_letter(request)
        raise UpstreamFailureError("openai", f"OpenAI error: {e.status_code}")
    except Exception as e:
        logger.error("cover_letter_unexpected", error=str(e))
        return _template_cover_letter(request)


def _system_prompt(tone: str) -> str:
    return f"""You are an expert cover letter writer for tech professionals.
{TONE_INSTRUCTIONS.get(tone, TONE_INSTRUCTIONS['professional'])}
RULES:
1. Only mention skills/experiences present in the CV
2. Be specific about the company and role
3. Mention 2-4 job-specific requirements
4. Keep it 300-500 words
5. No exaggeration or unsupported claims
6. Structure: Opening, 1-2 body paragraphs, closing"""


def _build_prompt(request: CoverLetterRequest, safe_description: str) -> str:
    return f"""Write a tailored cover letter.

CANDIDATE:
- Name: {request.user_profile.get('fullName', 'Candidate')}
- Summary: {request.user_profile.get('summary', 'N/A')}
- Skills: {', '.join(request.matched_skills[:8])}
- Experience: {request.user_profile.get('yearsOfExperience', 'N/A')} years as {request.user_profile.get('primaryRole', 'N/A')}

CV (excerpt):
{request.cv_text[:1500]}

JOB:
- Title: {request.job_title}
- Company: {request.job_company}
- Description: {safe_description[:2000]}

MATCHED: {', '.join(request.matched_skills[:5])}
MISSING: {', '.join(request.missing_skills[:5])}

Maximum {request.max_length} words."""


def _job_points(content: str, request: CoverLetterRequest) -> list[str]:
    points = []
    cl = content.lower()
    for s in request.matched_skills[:5]:
        if s.lower() in cl:
            points.append(f"Mentions {s}")
    if request.job_company.lower() in cl:
        points.append(f"References {request.job_company}")
    if request.job_title.lower() in cl:
        points.append(f"References {request.job_title}")
    return points


def _check_unsupported(content: str, request: CoverLetterRequest) -> list[str]:
    unsupported = []
    combined = f"{str(request.user_profile).lower()} {request.cv_text.lower()}"
    metric_pattern = r'(?:\d+(?:,\d{3})*(?:\+)?\s*(?:users?|customers?|revenue|million)|(?:revenue|users?|customers?)\s*(?:to|of)?\s*\d+(?:,\d{3})*)'
    for num in re.findall(metric_pattern, content.lower()):
        if num not in combined:
            unsupported.append(f"Unverified metric: {num}")
    return unsupported


def _template_cover_letter(request: CoverLetterRequest) -> CoverLetterResponse:
    name = request.user_profile.get("fullName", "Candidate")
    role = request.user_profile.get("primaryRole", "software engineer")
    skills = ", ".join(request.matched_skills[:5]) if request.matched_skills else "my technical skills"

    content = f"""Dear Hiring Manager,

I am writing to express my interest in the {request.job_title} position at {request.job_company}. With my background as a {role} and experience in {skills}, I am confident in my ability to contribute meaningfully to your team.

{'My technical expertise includes ' + skills + ', which aligns well with the requirements of this role. ' if request.matched_skills else ''}I am excited about the opportunity to bring my skills to {request.job_company} and contribute to your mission.

I would welcome the opportunity to discuss how my experience and skills align with your team's needs. Thank you for your consideration.

Best regards,
{name}"""

    return CoverLetterResponse(
        content=content,
        jobSpecificPoints=[f"References {request.job_company}", f"References {request.job_title}"],
        unsupportedClaims=[],
        confidenceScore=60.0,
    )
