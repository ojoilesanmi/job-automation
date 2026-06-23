from __future__ import annotations
import structlog
import re
from ..models.schemas import TextQualityRequest, TextQualityResponse

logger = structlog.get_logger()

GENERIC = [
    "i am a passionate", "i am a hard worker", "i am a team player",
    "i am detail oriented", "i am a fast learner", "i am motivated",
    "i am enthusiastic", "i am dedicated", "i am results driven",
    "excellent communication skills", "strong analytical skills",
    "detail-oriented professional", "team-oriented individual",
    "self-motivated individual", "go-getter", "think outside the box",
    "synergy", "leverage", "deep dive", "move the needle",
]

EXAGGERATIONS = [
    "the best", "the greatest", "world-class", "unparalleled",
    "unmatched", "industry-leading", "revolutionary", "groundbreaking",
    "10x", "best-in-class", "top 1%",
]


def analyze_text_quality(request: TextQualityRequest) -> TextQualityResponse:
    text = request.text
    issues = []
    suggestions = []
    wc = len(text.split())
    tl = text.lower()

    for phrase in GENERIC:
        if phrase in tl:
            issues.append({"type": "generic", "severity": "medium", "message": f"Generic: '{phrase}'"})

    for m in EXAGGERATIONS:
        if m.lower() in tl:
            issues.append({"type": "exaggeration", "severity": "high", "message": f"Exaggeration: '{m}'"})

    sentences = [s.strip() for s in re.split(r'[.!?]+', text) if s.strip()]
    long = [s for s in sentences if len(s.split()) > 30]
    if long:
        issues.append({"type": "sentence_length", "severity": "low", "message": f"{len(long)} long sentence(s)"})

    passive = re.findall(r'\b(was|were|been|being|is|are|am)\s+\w+ed\b', text, re.IGNORECASE)
    if len(passive) > 3:
        issues.append({"type": "passive_voice", "severity": "low", "message": f"Passive voice {len(passive)} times"})

    # Reading level (simplified Flesch-Kincaid)
    sc = max(len(sentences), 1)
    syllables = sum(max(len(re.findall(r'[aeiouy]+', w.lower())), 1) for w in text.split())
    fk = 206.835 - 1.015 * (wc / sc) - 84.6 * (syllables / max(wc, 1))
    reading = "easy" if fk >= 80 else "moderate" if fk >= 60 else "difficult" if fk >= 40 else "very_difficult"

    score = max(0.0, min(100.0, 100 - sum(15 if i["severity"] == "high" else 8 if i["severity"] == "medium" else 3 for i in issues)))

    if issues:
        if any(i["type"] == "generic" for i in issues):
            suggestions.append("Replace generic phrases with specific examples")
        if any(i["type"] == "exaggeration" for i in issues):
            suggestions.append("Back claims with measurable achievements")
    else:
        suggestions.append("Text quality looks good")

    logger.info("text_quality", word_count=wc, issues=len(issues), score=score)

    return TextQualityResponse(
        overallScore=round(score, 1), issues=issues, suggestions=suggestions,
        wordCount=wc, readingLevel=reading,
    )
