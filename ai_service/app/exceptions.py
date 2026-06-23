from fastapi import HTTPException


class AiServiceError(HTTPException):
    def __init__(self, status_code: int, detail: str, error_code: str = None):
        super().__init__(status_code=status_code, detail=detail)
        self.error_code = error_code


class ValidationError(AiServiceError):
    def __init__(self, detail: str):
        super().__init__(status_code=422, detail=detail, error_code="VALIDATION_ERROR")


class UpstreamTimeoutError(AiServiceError):
    def __init__(self, service: str = "openai"):
        super().__init__(status_code=408, detail=f"{service} request timed out", error_code="UPSTREAM_TIMEOUT")


class UpstreamFailureError(AiServiceError):
    def __init__(self, service: str = "openai", detail: str = None):
        super().__init__(
            status_code=502,
            detail=detail or f"{service} request failed",
            error_code="UPSTREAM_FAILURE",
        )


class RateLimitError(AiServiceError):
    def __init__(self, detail: str = "Rate limit exceeded"):
        super().__init__(status_code=429, detail=detail, error_code="RATE_LIMITED")


class ServiceUnavailableError(AiServiceError):
    def __init__(self, detail: str = "Service temporarily unavailable"):
        super().__init__(status_code=503, detail=detail, error_code="SERVICE_UNAVAILABLE")
