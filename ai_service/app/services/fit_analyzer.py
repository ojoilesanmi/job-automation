from __future__ import annotations
import structlog
import json
import re
from openai import AsyncOpenAI, APITimeoutError, APIStatusError, APIConnectionError
from ..config.settings import settings
from ..models.schemas import FitAnalysisRequest, FitAnalysisResponse
from ..exceptions import UpstreamFailureError, ServiceUnavailableError

logger = structlog.get_logger()

client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY) if settings.OPENAI_API_KEY else None


async def analyze_fit(request: FitAnalysisRequest) -> FitAnalysisResponse:
    logger.info("fit_analysis_start", job_title=request.job_title, company=request.job_company)

    if not client:
        logger.info("fit_analysis_fallback_rule_based")
        return _rule_based_fit(request)

    try:
        response = await client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {"role": "system", "content": "You are a job-matching analyst. Analyze fit factually. Never hallucinate skills not in the profile. Respond only in JSON."},
                {"role": "user", "content": _build_prompt(request)},
            ],
            temperature=0.3,
            max_tokens=1000,
        )

        content = response.choices[0].message.content
        if not content:
            logger.warning("fit_analysis_empty_response")
            return _rule_based_fit(request)

        parsed = _parse_response(content, request)
        logger.info("fit_analysis_complete", fit_score=parsed.fit_score)
        return parsed

    except APITimeoutError:
        logger.warning("fit_analysis_timeout_fallback")
        return _rule_based_fit(request)
    except APIConnectionError:
        raise ServiceUnavailableError("Cannot connect to OpenAI")
    except APIStatusError as e:
        if e.status_code == 429:
            raise UpstreamFailureError("openai", "OpenAI rate limit exceeded")
        if e.status_code >= 500:
            logger.warning("fit_analysis_upstream_error", status=e.status_code)
            return _rule_based_fit(request)
        raise UpstreamFailureError("openai", f"OpenAI error: {e.status_code}")
    except Exception as e:
        logger.error("fit_analysis_unexpected", error=str(e))
        return _rule_based_fit(request)


def _build_prompt(request: FitAnalysisRequest) -> str:
    return f"""Analyze the fit between this candidate and job.

CANDIDATE:
- Summary: {request.user_profile.get('summary', 'N/A')}
- Skills: {', '.join(request.user_skills)}
- Experience: {request.user_profile.get('yearsOfExperience', 'N/A')} years
- Role: {request.user_profile.get('primaryRole', 'N/A')}

JOB:
- Title: {request.job_title}
- Company: {request.job_company}
- Required Skills: {', '.join(request.job_required_skills)}
- Preferred Skills: {', '.join(request.job_preferred_skills)}
- Description: {request.job_description[:2000]}

Respond in this exact JSON format:
{{"fit_score": 0-100, "matched_skills": [], "missing_skills": [], "reasons_to_apply": [], "reasons_to_skip": [], "risk_flags": [], "explanation": ""}}"""


def _parse_response(content: str, request: FitAnalysisRequest) -> FitAnalysisResponse:
    match = re.search(r'\{.*\}', content, re.DOTALL)
    if match:
        try:
            d = json.loads(match.group())
            return FitAnalysisResponse(
                fitScore=min(100, max(0, d.get("fit_score", 50))),
                matchedSkills=d.get("matched_skills", []),
                missingSkills=d.get("missing_skills", []),
                reasonsToApply=d.get("reasons_to_apply", []),
                reasonsToSkip=d.get("reasons_to_skip", []),
                riskFlags=d.get("risk_flags", []),
                explanation=d.get("explanation", ""),
            )
        except (json.JSONDecodeError, KeyError) as e:
            logger.warning("fit_analysis_parse_error", error=str(e))
    return _rule_based_fit(request)


def _rule_based_fit(request: FitAnalysisRequest) -> FitAnalysisResponse:
    user = {s.lower() for s in request.user_skills}
    req = {s.lower() for s in request.job_required_skills}
    pref = {s.lower() for s in request.job_preferred_skills}

    matched = user & (req | pref)
    missing = req - user

    req_score = (len(req - missing) / max(len(req), 1)) * 35
    pref_score = (len(user & pref) / max(len(pref), 1)) * 15
    exp = request.user_profile.get("yearsOfExperience", 0) or 0
    exp_score = min(20, (exp / 5) * 20)
    score = round(min(100, req_score + pref_score + exp_score + 15), 1)

    reasons = []
    if matched:
        reasons.append(f"Matches: {', '.join(list(matched)[:5])}")
    if score >= 70:
        reasons.append("Strong overall fit")

    skip = []
    if missing:
        skip.append(f"Missing: {', '.join(list(missing)[:3])}")

    return FitAnalysisResponse(
        fitScore=score, matchedSkills=list(matched), missingSkills=list(missing),
        reasonsToApply=reasons, reasonsToSkip=skip, riskFlags=[],
        explanation=f"Rule-based: {len(matched)} matched, {len(missing)} missing",
    )
