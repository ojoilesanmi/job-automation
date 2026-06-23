from __future__ import annotations
import structlog
import re
import ipaddress
import socket
from urllib.parse import urlparse
import httpx
from ..exceptions import ValidationError
from ..constants import TECH_SKILLS

logger = structlog.get_logger()

# SSRF protection: block private/internal IPs
BLOCKED_HOSTS = {"localhost", "127.0.0.1", "0.0.0.0", "metadata.google.internal"}
PRIVATE_NETWORKS = [
    ipaddress.ip_network("10.0.0.0/8"),
    ipaddress.ip_network("172.16.0.0/12"),
    ipaddress.ip_network("192.168.0.0/16"),
    ipaddress.ip_network("169.254.0.0/16"),  # link-local / cloud metadata
    ipaddress.ip_network("127.0.0.0/8"),
]


def _validate_url(url: str) -> None:
    parsed = urlparse(url)
    if parsed.scheme not in ("http", "https"):
        raise ValidationError("Only http/https URLs are allowed")

    hostname = parsed.hostname
    if not hostname or hostname.lower() in BLOCKED_HOSTS:
        raise ValidationError("This URL is not allowed")

    try:
        resolved = socket.getaddrinfo(hostname, None)
        for _, _, _, _, addr in resolved:
            ip = ipaddress.ip_address(addr[0])
            for net in PRIVATE_NETWORKS:
                if ip in net:
                    raise ValidationError("This URL resolves to a private/internal network")
    except (socket.gaierror, ValueError) as e:
        if isinstance(e, ValidationError):
            raise
        raise ValidationError(f"Cannot resolve hostname: {hostname}")


async def _download_bytes(url: str) -> bytes:
    _validate_url(url)
    async with httpx.AsyncClient(timeout=15, follow_redirects=True) as client:
        resp = await client.get(url)
        resp.raise_for_status()
        return resp.content


def _extract_text_from_pdf(data: bytes) -> str:
    try:
        import fitz
        doc = fitz.open(stream=data, filetype="pdf")
        text = "\n".join(page.get_text() for page in doc)
        doc.close()
        return text
    except Exception as e:
        logger.warning("pdf_parse_failed", error=str(e))
        return ""


def _extract_text_from_docx(data: bytes) -> str:
    import io
    try:
        from docx import Document
        doc = Document(io.BytesIO(data))
        return "\n".join(p.text for p in doc.paragraphs)
    except Exception as e:
        logger.warning("docx_parse_failed", error=str(e))
        return ""


def _extract_text_sync(url: str, file_type: str) -> str:
    """Fallback: fetch and parse synchronously for non-async contexts."""
    try:
        _validate_url(url)
        with httpx.Client(timeout=15, follow_redirects=True) as client:
            resp = client.get(url)
            resp.raise_for_status()
            data = resp.content
    except Exception as e:
        logger.warning("cv_download_failed", error=str(e))
        return ""

    if file_type == "pdf":
        return _extract_text_from_pdf(data)
    elif file_type == "docx":
        return _extract_text_from_docx(data)
    return data.decode("utf-8", errors="ignore")


async def parse_cv_text(file_url: str, file_type: str) -> dict:
    logger.info("parsing_cv", file_type=file_type, file_url=file_url[:80])

    if file_type not in ("pdf", "docx"):
        raise ValidationError("Unsupported file type. Must be 'pdf' or 'docx'")

    try:
        data = await _download_bytes(file_url)
    except httpx.TimeoutException:
        raise ValidationError("File download timed out")
    except httpx.HTTPStatusError as e:
        raise ValidationError(f"Failed to download file: HTTP {e.response.status_code}")
    except ValidationError:
        raise
    except Exception as e:
        logger.error("cv_download_error", error=str(e))
        raise ValidationError("Failed to download file")

    if not data or len(data) < 100:
        raise ValidationError("Downloaded file is empty or too small")

    if file_type == "pdf":
        raw_text = _extract_text_from_pdf(data)
    else:
        raw_text = _extract_text_from_docx(data)

    if not raw_text or len(raw_text.strip()) < 20:
        raise ValidationError("Could not extract sufficient text from the document")

    text = raw_text.strip()

    email_match = re.search(r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}', text)
    email = email_match.group(0) if email_match else None

    phone_match = re.search(r'[\+]?[\d\s\-\(\)]{7,20}', text)
    phone = phone_match.group(0).strip() if phone_match else None

    lines = [l.strip() for l in text.split('\n') if l.strip()]
    name = None
    for line in lines[:5]:
        if len(line.split()) <= 4 and not re.search(r'[@\d]|http|www', line):
            name = line
            break

    location_match = re.search(r'(?:Location|Address|City|Based in)[:\s]+([^\n]+)', text, re.IGNORECASE)
    location = location_match.group(1).strip() if location_match else None

    return {
        "fullName": name,
        "email": email,
        "phone": phone,
        "location": location,
        "summary": _extract_summary(text),
        "skills": _extract_skills(text),
        "workExperience": _extract_work_experience(text),
        "projects": _extract_projects(text),
        "education": _extract_education(text),
        "certifications": [],
        "links": re.findall(r'https?://[^\s\)]+', text),
    }


def _extract_skills(text: str) -> list[str]:
    skills_section = re.search(
        r'(?:skills|technologies|technical skills|core competencies)[:\s]*\n(.*?)(?:\n\s*\n|\n[A-Z])',
        text, re.IGNORECASE | re.DOTALL,
    )
    if skills_section:
        raw = re.split(r'[,•\|\n]+', skills_section.group(1))
        return list(dict.fromkeys(s.strip().strip('- *') for s in raw if s.strip()))

    text_lower = text.lower()
    return [s for s in TECH_SKILLS if s in text_lower]


def _extract_work_experience(text: str) -> list[dict]:
    pattern = re.compile(
        r'([A-Z][^\n]{5,60})\s+(?:at|@|\||–|-)\s+([A-Z][^\n]{2,60})\s*[\(\[]?(\w+\s+\d{4})\s*[-–]\s*(\w+\s+\d{4}|Present|Current)[\)\]]?',
        re.MULTILINE,
    )
    return [
        {"title": m.group(1).strip(), "company": m.group(2).strip(),
         "startDate": m.group(3).strip(), "endDate": m.group(4).strip()}
        for m in pattern.finditer(text)
    ]


def _extract_education(text: str) -> list[dict]:
    edu = re.compile(
        r'(Bachelor|Master|PhD|B\.S\.|M\.S\.|B\.A\.|M\.A\.|B\.Tech|M\.Tech)[^\n]*(?:from|at)\s+([^\n]+)',
        re.IGNORECASE,
    )
    return [{"degree": m.group(1).strip(), "institution": m.group(2).strip()} for m in edu.finditer(text)]


def _extract_projects(text: str) -> list[dict]:
    match = re.search(
        r'(?:projects?|personal projects?)[:\s]*\n(.*?)(?:\n\s*\n|\n[A-Z][A-Z])',
        text, re.IGNORECASE | re.DOTALL,
    )
    if not match:
        return []
    return [
        {"name": re.sub(r'^[-•*]\s*', '', l).strip()[:80], "description": re.sub(r'^[-•*]\s*', '', l).strip()}
        for l in match.group(1).split('\n') if l.strip() and len(l.strip()) > 10
    ][:5]


def _extract_summary(text: str) -> str | None:
    match = re.search(
        r'(?:summary|profile|about|objective)[:\s]*\n(.*?)(?:\n\s*\n)',
        text, re.IGNORECASE | re.DOTALL,
    )
    return match.group(1).strip()[:500] if match else None
