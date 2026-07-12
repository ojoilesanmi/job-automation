import pytest
from app.services.job_parser import parse_job_description
from app.exceptions import ValidationError


class TestJobParser:
    def test_parse_valid_job_description(self):
        request = {
            "title": "Senior Software Engineer",
            "company": "Tech Corp",
            "description": """
            We are looking for a Senior Software Engineer with 5+ years of experience.
            Requirements:
            - Strong knowledge of Python and Java
            - Experience with AWS and Docker
            - Familiarity with microservices architecture
            
            Nice to have:
            - Kubernetes experience
            - GraphQL knowledge
            
            This is a fully remote position with relocation assistance available.
            Salary: $150,000 - $200,000 USD
            """
        }
        result = parse_job_description(request)
        
        assert result["cleanedDescription"] is not None
        assert len(result["cleanedDescription"]) > 0
        assert "python" in [s.lower() for s in result["requiredSkills"]]
        assert "java" in [s.lower() for s in result["requiredSkills"]]
        assert result["remoteType"] == "full_remote"
        assert result["relocationAvailable"] is True
        assert result["salaryMin"] == 150000
        assert result["salaryMax"] == 200000
        assert result["currency"] == "USD"
        assert result["seniority"] == "senior"
        assert result["experienceYears"] == 5

    def test_parse_empty_description_raises_error(self):
        request = {
            "title": "Software Engineer",
            "company": "Tech Corp",
            "description": ""
        }
        with pytest.raises(ValidationError):
            parse_job_description(request)

    def test_parse_short_description_raises_error(self):
        request = {
            "title": "Software Engineer",
            "company": "Tech Corp",
            "description": "Short"
        }
        with pytest.raises(ValidationError):
            parse_job_description(request)

    def test_detect_remote_type(self):
        test_cases = [
            ("We are a fully remote company", "full_remote"),
            ("Work from home available", "remote"),
            ("Hybrid work environment", "hybrid"),
            ("In office position", "onsite"),
        ]
        for desc, expected in test_cases:
            request = {"title": "Engineer", "company": "Co", "description": f"Requirements: {desc}. " * 5}
            result = parse_job_description(request)
            assert result["remoteType"] == expected, f"Failed for: {desc}"

    def test_detect_seniority_levels(self):
        test_cases = [
            ("Staff Engineer", "staff"),
            ("Senior Developer", "senior"),
            ("Junior Developer", "junior"),
            ("Software Engineer", "mid"),
        ]
        for title, expected in test_cases:
            request = {"title": title, "company": "Co", "description": "Requirements: Python, Java. " * 5}
            result = parse_job_description(request)
            assert result["seniority"] == expected, f"Failed for: {title}"

    def test_extract_experience_years(self):
        request = {
            "title": "Engineer",
            "company": "Co",
            "description": "Requirements: Python. We need 7+ years of experience with backend development. " * 3
        }
        result = parse_job_description(request)
        assert result["experienceYears"] == 7

    def test_extract_salary_euro(self):
        request = {
            "title": "Engineer",
            "company": "Co",
            "description": "Requirements: Python. Salary: €80,000 - €120,000 per year. " * 3
        }
        result = parse_job_description(request)
        assert result["currency"] == "EUR"
        assert result["salaryMin"] == 80000
        assert result["salaryMax"] == 120000

    def test_detect_employment_type(self):
        test_cases = [
            ("Full-time position", "full_time"),
            ("Part-time role", "part_time"),
            ("Contract work", "contract"),
            ("Internship available", "internship"),
        ]
        for desc, expected in test_cases:
            request = {"title": "Engineer", "company": "Co", "description": f"Requirements: Python. {desc}. " * 5}
            result = parse_job_description(request)
            assert result["employmentType"] == expected, f"Failed for: {desc}"

    def test_visa_sponsorship_detection(self):
        request = {
            "title": "Engineer",
            "company": "Co",
            "description": "Requirements: Python. We offer visa sponsorship for international candidates. " * 3
        }
        result = parse_job_description(request)
        assert result["visaSponsorship"] is True

    def test_html_tags_stripped(self):
        request = {
            "title": "Engineer",
            "company": "Co",
            "description": "Requirements: <b>Python</b> and <script>alert('xss')</script> Java experience. " * 5
        }
        result = parse_job_description(request)
        assert "<script>" not in result["cleanedDescription"]
        assert "<b>" not in result["cleanedDescription"]
