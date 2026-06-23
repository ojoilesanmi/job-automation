import structlog
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from prometheus_fastapi_instrumentator import Instrumentator

from .config.settings import settings
from .routes.ai import router as ai_router
from .routes.health import router as health_router
from .exceptions import AiServiceError
from .error_handlers import ai_service_error_handler, unhandled_error_handler
from .middleware import ApiKeyMiddleware, RequestLoggingMiddleware

structlog.configure(
    processors=[
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.add_log_level,
        structlog.processors.JSONRenderer(),
    ],
)
logger = structlog.get_logger()

app = FastAPI(title=settings.APP_NAME, version=settings.APP_VERSION)

# CORS - no wildcard with credentials
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:3001"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Custom middleware
app.add_middleware(ApiKeyMiddleware)
app.add_middleware(RequestLoggingMiddleware)

# Error handlers
app.add_exception_handler(AiServiceError, ai_service_error_handler)
app.add_exception_handler(Exception, unhandled_error_handler)

# Prometheus
Instrumentator().instrument(app).expose(app, endpoint="/metrics")

# Routes
app.include_router(health_router, tags=["health"])
app.include_router(ai_router, prefix="/api/v1/ai", tags=["ai"])
