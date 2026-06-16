import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.engine.client import EngineClient
from app.llm.router import LlmRouter
from app.routes.chat import router as chat_router
from app.session.store import RateLimiter, SessionStore

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.engine_client = EngineClient(settings.aifa_api_base_url)
    app.state.rate_limiter = RateLimiter(settings.redis_url, settings.ai_daily_limit)
    app.state.session_store = SessionStore(settings.redis_url)
    app.state.llm_router = LlmRouter()
    await app.state.rate_limiter.connect()
    await app.state.session_store.connect()
    logger.info("AIFA orchestrator started (api=%s)", settings.aifa_api_base_url)
    yield
    await app.state.rate_limiter.close()
    await app.state.session_store.close()


app = FastAPI(title="AIFA AI Orchestrator", version="0.1.0", lifespan=lifespan)

origins = [o.strip() for o in settings.cors_origins.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins if origins != ["*"] else ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat_router)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}
