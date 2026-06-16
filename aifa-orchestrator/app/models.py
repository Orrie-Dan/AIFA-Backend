from typing import Any, Literal

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)


class ChatResponse(BaseModel):
    reply: str
    intent: str
    engineData: dict[str, Any] | list[Any] | None = None
    source: Literal["rule_based", "llm", "llm_private", "fallback", "clarification"]


class ProblemDetail(BaseModel):
    type: str
    title: str
    status: int
    detail: str
    instance: str
