from fastapi import Request, Response
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
import structlog
import time

from .config.settings import settings

logger = structlog.get_logger()


class ApiKeyMiddleware(BaseHTTPMiddleware):
    EXEMPT_PATHS = {"/docs", "/redoc", "/metrics", "/api/v1/ai/health"}

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        if request.url.path in self.EXEMPT_PATHS:
            return await call_next(request)

        if settings.API_KEY:
            key = request.headers.get("X-API-Key")
            if key != settings.API_KEY:
                return JSONResponse(
                    status_code=401,
                    content={
                        "responseCode": "UNAUTHORIZED",
                        "responseStatus": "error",
                        "responseMessage": "Invalid or missing API key",
                        "data": None,
                    },
                )

        return await call_next(request)


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        import uuid
        cid = str(uuid.uuid4())
        request.state.correlation_id = cid
        start = time.time()
        response = await call_next(request)
        duration = round((time.time() - start) * 1000, 2)
        logger.info(
            "request",
            cid=cid,
            method=request.method,
            path=request.url.path,
            status=response.status_code,
            ms=duration,
        )
        response.headers["X-Correlation-ID"] = cid
        return response
