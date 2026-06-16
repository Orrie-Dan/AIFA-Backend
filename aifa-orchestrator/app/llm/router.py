from typing import Any

from app.llm.ollama_client import OllamaClient
from app.llm.openai_client import OpenAIClient


class LlmRouter:
    def __init__(self) -> None:
        self._openai = OpenAIClient()
        self._ollama = OllamaClient()

    async def generate(
        self,
        ai_mode: str,
        user_message: str,
        engine_data: dict[str, Any],
        history: list[dict[str, str]],
        user_hash: str,
        retry: bool = False,
    ) -> tuple[str, str]:
        if ai_mode == "private_mode":
            if await self._ollama.available():
                text = await self._ollama.generate(user_message, engine_data, history, retry=retry)
                return text, "llm_private"
            raise RuntimeError("Ollama unavailable for private mode")

        if self._openai.available:
            text = await self._openai.generate(
                user_message, engine_data, history, user_hash, retry=retry
            )
            return text, "llm"

        raise RuntimeError("OpenAI unavailable for smart mode")
