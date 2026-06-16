import json
from datetime import date, datetime, timezone

import redis.asyncio as redis

from app.config import settings


class RateLimiter:
    def __init__(self, redis_url: str, daily_limit: int):
        self._redis_url = redis_url
        self._daily_limit = daily_limit
        self._client: redis.Redis | None = None

    async def connect(self) -> None:
        self._client = redis.from_url(self._redis_url, decode_responses=True)

    async def close(self) -> None:
        if self._client:
            await self._client.aclose()

    async def check_and_increment(self, user_id: str, exempt: bool = False) -> tuple[bool, int]:
        if exempt:
            return True, 0
        if not self._client:
            await self.connect()
        assert self._client is not None
        key = f"ai:rate:{user_id}:{date.today().isoformat()}"
        count = await self._client.incr(key)
        if count == 1:
            await self._client.expire(key, 86400)
        remaining = max(0, self._daily_limit - count)
        return count <= self._daily_limit, remaining


class SessionStore:
    def __init__(self, redis_url: str, max_turns: int = 10, ttl_seconds: int = 3600):
        self._redis_url = redis_url
        self._max_turns = max_turns
        self._ttl_seconds = ttl_seconds
        self._client: redis.Redis | None = None

    async def connect(self) -> None:
        self._client = redis.from_url(self._redis_url, decode_responses=True)

    async def close(self) -> None:
        if self._client:
            await self._client.aclose()

    async def get_history(self, user_id: str) -> list[dict[str, str]]:
        if not self._client:
            await self.connect()
        assert self._client is not None
        raw = await self._client.get(f"ai:session:{user_id}")
        if not raw:
            return []
        return json.loads(raw)

    async def append_turn(self, user_id: str, role: str, content: str) -> None:
        if not self._client:
            await self.connect()
        assert self._client is not None
        history = await self.get_history(user_id)
        history.append({"role": role, "content": content})
        history = history[-self._max_turns :]
        key = f"ai:session:{user_id}"
        await self._client.set(key, json.dumps(history), ex=self._ttl_seconds)

    async def get_snapshot(self, user_id: str) -> dict | None:
        if not self._client:
            await self.connect()
        assert self._client is not None
        raw = await self._client.get(f"ai:snapshot:{user_id}")
        return json.loads(raw) if raw else None

    async def set_snapshot(self, user_id: str, snapshot: dict) -> None:
        if not self._client:
            await self.connect()
        assert self._client is not None
        await self._client.set(
            f"ai:snapshot:{user_id}",
            json.dumps(snapshot),
            ex=86400,
        )


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()
