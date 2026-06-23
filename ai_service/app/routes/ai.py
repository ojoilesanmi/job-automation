from __future__ import annotations
from fastapi import APIRouter

from ..models.schemas import (
    CvParseRequest, CvParseResponse,
    JobParseRequest, JobParseResponse,
    FitAnalysisRequest, FitAnalysisResponse,
    CoverLetterRequest, CoverLetterResponse,
    InjectionCheckRequest, InjectionCheckResponse,
    TextQualityRequest, TextQualityResponse,
)
from ..services.cv_parser import parse_cv_text
from ..services.job_parser import parse_job_description
from ..services.fit_analyzer import analyze_fit
from ..services.cover_letter import generate_cover_letter
from ..services.injection_guard import check_injection
from ..services.text_quality import analyze_text_quality

router = APIRouter()


@router.post("/parse-cv", response_model=CvParseResponse)
async def parse_cv(request: CvParseRequest):
    return await parse_cv_text(request.file_url, request.file_type)


@router.post("/parse-job", response_model=JobParseResponse)
async def parse_job(request: JobParseRequest):
    return parse_job_description(request.model_dump())


@router.post("/analyze-fit", response_model=FitAnalysisResponse)
async def fit_analysis(request: FitAnalysisRequest):
    return await analyze_fit(request)


@router.post("/generate-cover-letter", response_model=CoverLetterResponse)
async def cover_letter_generation(request: CoverLetterRequest):
    return await generate_cover_letter(request)


@router.post("/check-injection", response_model=InjectionCheckResponse)
async def injection_check(request: InjectionCheckRequest):
    return check_injection(request)


@router.post("/check-quality", response_model=TextQualityResponse)
async def quality_check(request: TextQualityRequest):
    return analyze_text_quality(request)
