import logging
from typing import Any

from fastapi import APIRouter, Header, HTTPException, Request

from app.auth.jwt import AuthError, parse_authorization_header
from app.config import settings
from app.context.builder import anonymize_for_llm
from app.engine.client import EngineClient, EngineClientError
from app.intent.classifier import Intent, classify_intent
from app.intent.params import extract_affordability_params, missing_affordability_fields
from app.llm.router import LlmRouter
from app.models import ChatRequest, ChatResponse, ProblemDetail
from app.responses.templates import build_clarifying_question, build_rule_based_reply
from app.session.store import RateLimiter, SessionStore
from app.validator.output_validator import OutputValidator

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1")


def _get_engine_client(request: Request) -> EngineClient:
    return request.app.state.engine_client


def _get_rate_limiter(request: Request) -> RateLimiter:
    return request.app.state.rate_limiter


def _get_session_store(request: Request) -> SessionStore:
    return request.app.state.session_store


def _get_llm_router(request: Request) -> LlmRouter:
    return request.app.state.llm_router


@router.post("/chat", response_model=ChatResponse)
async def chat(
    body: ChatRequest,
    request: Request,
    authorization: str | None = Header(default=None),
) -> ChatResponse:
    try:
        token_info = parse_authorization_header(authorization)
    except AuthError as exc:
        raise HTTPException(
            status_code=401,
            detail=ProblemDetail(
                type="https://aifa.rw/problems/unauthorized",
                title="Unauthorized",
                status=401,
                detail=str(exc),
                instance=str(request.url.path),
            ).model_dump(),
        ) from exc

    engine = _get_engine_client(request)
    rate_limiter = _get_rate_limiter(request)
    session_store = _get_session_store(request)
    llm_router = _get_llm_router(request)
    validator = OutputValidator()

    intent = classify_intent(body.message)
    affordability_params = None
    clarifying = False

    if intent == Intent.AFFORDABILITY:
        affordability_params = extract_affordability_params(body.message)
        if affordability_params is None:
            missing = missing_affordability_fields(body.message)
            if missing:
                reply = build_clarifying_question(missing)
                await session_store.append_turn(token_info.user_id, "user", body.message)
                await session_store.append_turn(token_info.user_id, "assistant", reply)
                return ChatResponse(
                    reply=reply,
                    intent=intent.value,
                    engineData=None,
                    source="clarification",
                )

    allowed, remaining = await rate_limiter.check_and_increment(
        token_info.user_id,
        exempt=clarifying,
    )
    if not allowed:
        raise HTTPException(
            status_code=429,
            detail=ProblemDetail(
                type="https://aifa.rw/problems/rate-limit",
                title="Too Many Requests",
                status=429,
                detail=f"Daily AI query limit reached ({settings.ai_daily_limit}/day).",
                instance=str(request.url.path),
            ).model_dump(),
        )

    try:
        profile = await engine.get_profile(token_info.raw)
        ai_mode = profile.get("aiMode", "smart")
        engine_data: dict[str, Any] | list[Any]
        engine_data = await engine.execute_intent(
            intent,
            token_info.raw,
            affordability=affordability_params,
        )
    except EngineClientError as exc:
        raise HTTPException(
            status_code=exc.status_code,
            detail=ProblemDetail(
                type="https://aifa.rw/problems/engine-error",
                title="Engine Error",
                status=exc.status_code,
                detail=exc.detail,
                instance=str(request.url.path),
            ).model_dump(),
        ) from exc

    if isinstance(engine_data, list):
        normalized: dict[str, Any] = {"items": engine_data}
    else:
        normalized = engine_data

    if intent == Intent.GENERAL:
        await session_store.set_snapshot(token_info.user_id, normalized)

    history = await session_store.get_history(token_info.user_id)
    anonymized = anonymize_for_llm(intent.value, normalized)
    fallback_reply = build_rule_based_reply(intent, normalized)

    source = "rule_based"
    reply = fallback_reply

    try:
        llm_text, llm_source = await llm_router.generate(
            ai_mode=str(ai_mode),
            user_message=body.message,
            engine_data=anonymized,
            history=history,
            user_hash=token_info.user_hash,
            retry=False,
        )
        validation = validator.validate(llm_text, normalized)
        if validation.passed:
            reply = llm_text
            source = llm_source
        else:
            llm_text_retry, llm_source_retry = await llm_router.generate(
                ai_mode=str(ai_mode),
                user_message=body.message,
                engine_data=anonymized,
                history=history,
                user_hash=token_info.user_hash,
                retry=True,
            )
            validation_retry = validator.validate(llm_text_retry, normalized)
            if validation_retry.passed:
                reply = llm_text_retry
                source = llm_source_retry
            else:
                reply = fallback_reply
                source = "fallback"
                logger.warning(
                    "LLM validation failed for user %s intent %s issue %s",
                    token_info.user_id,
                    intent.value,
                    validation_retry.issue,
                )
    except RuntimeError:
        reply = fallback_reply
        source = "rule_based"

    await session_store.append_turn(token_info.user_id, "user", body.message)
    await session_store.append_turn(token_info.user_id, "assistant", reply)

    return ChatResponse(
        reply=reply,
        intent=intent.value,
        engineData=normalized,
        source=source,  # type: ignore[arg-type]
    )
