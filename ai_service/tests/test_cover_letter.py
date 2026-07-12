import pytest
from app.services.cover_letter import (
    _system_prompt,
    _build_prompt,
    _check_unsupported,
    _job_points,
    _template_cover_letter,
)
from app.models.schemas import CoverLetterRequest


class TestCoverLetterPromptConstruction:
    def test_system_prompt_professional(self):
        prompt = _system_prompt("professional")
        assert "expert cover letter writer" in prompt.lower()
        assert "formal, professional tone" in prompt.lower()
        assert "Only mention skills/experiences present in the CV" in prompt

    def test_system_prompt_enthusiastic(self):
        prompt = _system_prompt("enthusiastic")
        assert "enthusiasm and energy" in prompt.lower()

    def test_system_prompt_technical(self):
        prompt = _system_prompt("technical")
        assert "technical depth" in prompt.lower()

    def test_build_prompt_includes_candidate_info(self):
        request = CoverLetterRequest(
            user_profile={"fullName": "John Doe", "summary": "Senior engineer", "yearsOfExperience": 8, "primaryRole": "Backend Engineer"},
            cv_text="Python expert with 8 years experience",
            job_title="Senior Developer",
            job_company="TechCo",
            job_description="Looking for a senior developer",
            matched_skills=["Python", "Java"],
            missing_skills=["Go"],
            tone="professional",
            max_length=500,
        )
        prompt = _build_prompt(request, "Looking for a senior developer")
        
        assert "John Doe" in prompt
        assert "Senior Developer" in prompt
        assert "TechCo" in prompt
        assert "Python" in prompt
        assert "Java" in prompt

    def test_build_prompt_sanitizes_description(self):
        request = CoverLetterRequest(
            user_profile={"fullName": "Jane"},
            cv_text="Experienced engineer",
            job_title="Engineer",
            job_company="Co",
            job_description="Normal description",
            matched_skills=[],
            missing_skills=[],
            tone="professional",
            max_length=500,
        )
        safe_desc = "Sanitized description"
        prompt = _build_prompt(request, safe_desc)
        assert "Sanitized description" in prompt


class TestCoverLetterClaimValidation:
    def test_check_unsupported_with_clean_content(self):
        request = CoverLetterRequest(
            user_profile={"fullName": "John"},
            cv_text="Built a platform serving 10000 users",
            job_title="Engineer",
            job_company="Co",
            job_description="desc",
            matched_skills=[],
            missing_skills=[],
            tone="professional",
            max_length=500,
        )
        content = "I have experience building platforms"
        unsupported = _check_unsupported(content, request)
        assert len(unsupported) == 0

    def test_check_unsupported_with_unverified_metric(self):
        request = CoverLetterRequest(
            user_profile={"fullName": "John"},
            cv_text="Built a platform",
            job_title="Engineer",
            job_company="Co",
            job_description="desc",
            matched_skills=[],
            missing_skills=[],
            tone="professional",
            max_length=500,
        )
        content = "I grew revenue to 5,000,000"
        unsupported = _check_unsupported(content, request)
        assert len(unsupported) > 0
        assert "5,000,000" in unsupported[0]

    def test_job_points_identifies_matched_skills(self):
        request = CoverLetterRequest(
            user_profile={"fullName": "John"},
            cv_text="skills",
            job_title="Engineer",
            job_company="TechCo",
            job_description="desc",
            matched_skills=["Python", "Java", "AWS"],
            missing_skills=[],
            tone="professional",
            max_length=500,
        )
        content = "My Python experience aligns with your needs at TechCo"
        points = _job_points(content, request)
        assert any("Python" in p for p in points)
        assert any("TechCo" in p for p in points)

    def test_job_points_empty_when_no_matches(self):
        request = CoverLetterRequest(
            user_profile={"fullName": "John"},
            cv_text="skills",
            job_title="Engineer",
            job_company="TechCo",
            job_description="desc",
            matched_skills=[],
            missing_skills=[],
            tone="professional",
            max_length=500,
        )
        content = "I am interested in the position"
        points = _job_points(content, request)
        assert len(points) == 0


class TestCoverLetterTemplate:
    def test_template_includes_candidate_name(self):
        request = CoverLetterRequest(
            user_profile={"fullName": "John Doe", "primaryRole": "Developer"},
            cv_text="skills",
            job_title="Engineer",
            job_company="TechCo",
            job_description="desc",
            matched_skills=["Python"],
            missing_skills=[],
            tone="professional",
            max_length=500,
        )
        response = _template_cover_letter(request)
        assert "John Doe" in response.content
        assert "Engineer" in response.content
        assert "TechCo" in response.content
        assert response.confidenceScore == 60.0

    def test_template_handles_missing_skills(self):
        request = CoverLetterRequest(
            user_profile={"fullName": "John"},
            cv_text="skills",
            job_title="Engineer",
            job_company="TechCo",
            job_description="desc",
            matched_skills=[],
            missing_skills=[],
            tone="professional",
            max_length=500,
        )
        response = _template_cover_letter(request)
        assert "my technical skills" in response.content.lower()
