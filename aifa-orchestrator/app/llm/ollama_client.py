import json
from typing import Any

import httpx

from app.config import settings
from app.llm.openai_client import SYSTEM_PROMPT


class OllamaClient:
    def __init__(self) -> None:
        self._base_url = settings.ollama_base_url.rstrip("/")
        self._model = settings.ollama_model

    async def available(self) -> bool:
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{self._base_url}/api/tags")
                return response.status_code == 200
        except httpx.HTTPError:
            return False

    async def generate(
        self,
        user_message: str,
        engine_data: dict[str, Any],
        history: list[dict[str, str]],
        retry: bool = False,
    ) -> str:
        engine_json = json.dumps(engine_data, indent=2)
        history_text = "\n".join(f"{turn['role']}: {turn['content']}" for turn in history[-10:])
        prompt = (
            f"{SYSTEM_PROMPT}\n\n"
            f"[DATA]\n{engine_json}\n\n"
            f"[CONVERSATION HISTORY]\n{history_text}\n\n"
            f"[USER QUESTION]\n{user_message}"
        )
        if retry:
            prompt = "Rewrite using ONLY numbers in DATA.\n\n" + prompt

        payload = {
            "model": self._model,
            "prompt": prompt,
            "stream": False,
            "options": {"temperature": 0.2},
        }
        async with httpx.AsyncClient(timeout=120.0) as client:
            response = await client.post(f"{self._base_url}/api/generate", json=payload)
            response.raise_for_status()
            data = response.json()
        return data.get("response", "")
