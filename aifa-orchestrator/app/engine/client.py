from typing import Any

import httpx

from app.intent.classifier import Intent
from app.intent.params import AffordabilityParams


class EngineClientError(Exception):
    def __init__(self, status_code: int, detail: str):
        self.status_code = status_code
        self.detail = detail
        super().__init__(detail)


class EngineClient:
    def __init__(self, base_url: str):
        self._base_url = base_url.rstrip("/")

    async def get_profile(self, token: str) -> dict[str, Any]:
        return await self._request("GET", "/api/v1/users/me", token)

    async def run_affordability(self, token: str, params: AffordabilityParams) -> dict[str, Any]:
        body = {
            "itemPriceRwf": params.item_price_rwf,
            "targetDate": params.target_date.isoformat(),
        }
        return await self._request("POST", "/api/v1/insights/affordability", token, json=body)

    async def get_spending_analysis(self, token: str) -> dict[str, Any]:
        return await self._request("GET", "/api/v1/insights/spending-analysis", token)

    async def get_health_score(self, token: str) -> dict[str, Any]:
        return await self._request("GET", "/api/v1/insights/health-score", token)

    async def get_budgets(self, token: str) -> list[dict[str, Any]]:
        return await self._request("GET", "/api/v1/budgets/current", token)

    async def get_goals(self, token: str) -> list[dict[str, Any]]:
        return await self._request("GET", "/api/v1/goals", token)

    async def get_recommendations(self, token: str) -> list[dict[str, Any]]:
        return await self._request("GET", "/api/v1/insights/recommendations", token)

    async def get_dashboard_summary(self, token: str) -> dict[str, Any]:
        return await self._request("GET", "/api/v1/dashboard/summary", token)

    async def execute_intent(
        self,
        intent: Intent,
        token: str,
        affordability: AffordabilityParams | None = None,
    ) -> dict[str, Any]:
        if intent == Intent.AFFORDABILITY:
            if affordability is None:
                raise ValueError("affordability params required")
            return await self.run_affordability(token, affordability)
        if intent == Intent.SPENDING:
            return await self.get_spending_analysis(token)
        if intent == Intent.HEALTH_SCORE:
            return await self.get_health_score(token)
        if intent == Intent.BUDGET_STATUS:
            return {"budgets": await self.get_budgets(token)}
        if intent == Intent.GOALS:
            return {"goals": await self.get_goals(token)}
        if intent == Intent.RECOMMENDATIONS:
            return {"recommendations": await self.get_recommendations(token)}
        return await self.get_dashboard_summary(token)

    async def _request(
        self,
        method: str,
        path: str,
        token: str,
        json: dict[str, Any] | None = None,
    ) -> Any:
        headers = {"Authorization": f"Bearer {token}"}
        async with httpx.AsyncClient(base_url=self._base_url, timeout=30.0) as client:
            response = await client.request(method, path, headers=headers, json=json)
        if response.status_code >= 400:
            detail = response.text
            try:
                payload = response.json()
                detail = payload.get("detail", detail)
            except ValueError:
                pass
            raise EngineClientError(response.status_code, str(detail))
        if response.status_code == 204:
            return {}
        return response.json()
