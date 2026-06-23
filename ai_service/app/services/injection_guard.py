from __future__ import annotations
import structlog
import re
from ..models.schemas import InjectionCheckRequest, InjectionCheckResponse

logger = structlog.get_logger()

# More specific patterns - avoid flagging legitimate content
PATTERNS = [
    (r"ignore\s+(previous|all|above)\s+(instructions?|prompts?|rules?)", "instruction_override"),
    (r"you\s+are\s+now\s+(a|an)\s+", "role_hijack"),
    r"system\s*:\s*",
    r"act\s+as\s+if\s+",
    r"pretend\s+you\s+(are|were)\s+",
    r"disregard\s+(all|any|previous)\s+",
    r"override\s+(your|the|all)\s+",
    r"new\s+instructions?\s*:",
    r"forget\s+(everything|all|previous)",
    r"roleplay\s+as\s+",
    r"jailbreak",
    r"DAN\s+mode",
    r"developer\s+mode",
    r"<\|im_start\|>",
    r"<\|im_end\|>",
    r"Human:",
    r"Assistant:",
    r"\b(drop\s+table)\b",
    r"<script",
    r"javascript:",
]

COMPILED = []
for p in PATTERNS:
    pattern = p[0] if isinstance(p, tuple) else p
    COMPILED.append(re.compile(pattern, re.IGNORECASE))


def check_injection(request: InjectionCheckRequest) -> InjectionCheckResponse:
    text = request.text
    flagged = [p.pattern for p in COMPILED if p.search(text)]

    risk = "high" if len(flagged) >= 3 else "medium" if flagged else "low"
    safe = risk == "low"

    sanitized = text
    if not safe:
        for p in COMPILED:
            sanitized = p.sub("[FILTERED]", sanitized)

    logger.info("injection_check", risk_level=risk, flagged=len(flagged))

    return InjectionCheckResponse(
        isSafe=safe,
        riskLevel=risk,
        flaggedPatterns=flagged,
        sanitizedText=sanitized if not safe else None,
    )
