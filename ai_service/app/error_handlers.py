import structlog
from fastapi import Request
from fastapi.responses import JSONResponse

from .exceptions import AiServiceError

logger = structlog.get_logger()


async def ai_service_error_handler(request: Request, exc: AiServiceError) -> JSONResponse:
    logger.warning("ai_error", status=exc.status_code, error_code=exc.error_code, detail=exc.detail)
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "responseCode": exc.error_code or str(exc.status_code),
            "responseStatus": "error",
            "responseMessage": exc.detail,
            "data": None,
        },
    )


async def unhandled_error_handler(request: Request, exc: Exception) -> JSONResponse:
    logger.error("unhandled_error", path=request.url.path, error=str(exc))
    return JSONResponse(
        status_code=500,
        content={
            "responseCode": "INTERNAL_ERROR",
            "responseStatus": "error",
            "responseMessage": "Internal server error",
            "data": None,
        },
    )
