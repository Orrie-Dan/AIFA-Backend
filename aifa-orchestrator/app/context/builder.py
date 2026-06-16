import copy
from typing import Any


_STRIP_KEYS = {
    "id",
    "walletId",
    "categoryId",
    "merchantName",
    "description",
    "userId",
    "batchId",
}


def anonymize_for_llm(intent: str, engine_data: dict[str, Any]) -> dict[str, Any]:
    data = copy.deepcopy(engine_data)
    if intent == "affordability":
        return _strip_affordability(data)
    if intent == "spending":
        return _strip_spending(data)
    if intent == "health_score":
        return _strip_health(data)
    if intent == "budget_status":
        return {"budgets": [_strip_budget(b) for b in data.get("budgets", [])]}
    if intent == "goals":
        return {"goals": [_strip_goal(g) for g in data.get("goals", [])]}
    if intent == "recommendations":
        return {"recommendations": data.get("recommendations", [])}
    return _strip_dashboard(data)


def _strip_affordability(data: dict[str, Any]) -> dict[str, Any]:
    return {
        "affordable": data.get("affordable"),
        "monthsAvailable": data.get("monthsAvailable"),
        "monthsNeededMinimum": data.get("monthsNeededMinimum"),
        "projectedSavingsAtTargetDateRwf": data.get("projectedSavingsAtTargetDateRwf"),
        "bufferAfterPurchaseRwf": data.get("bufferAfterPurchaseRwf"),
        "emergencyFundImpact": data.get("emergencyFundImpact"),
        "confidence": data.get("confidence"),
        "warnings": data.get("warnings", []),
    }


def _strip_spending(data: dict[str, Any]) -> dict[str, Any]:
    categories = []
    for item in data.get("categories", []):
        categories.append(
            {
                "categoryName": item.get("categoryName"),
                "currentMonthSpendRwf": item.get("currentMonthSpendRwf"),
                "baselineSpendRwf": item.get("baselineSpendRwf"),
                "momChangePercent": item.get("momChangePercent"),
                "alertLevel": item.get("alertLevel"),
                "alertMessage": item.get("alertMessage"),
            }
        )
    return {"status": data.get("status"), "categories": categories}


def _strip_health(data: dict[str, Any]) -> dict[str, Any]:
    return {
        "status": data.get("status"),
        "score": data.get("score"),
        "bandLabel": data.get("bandLabel"),
        "topDriver": data.get("topDriver"),
        "topImprovement": data.get("topImprovement"),
        "components": data.get("components"),
    }


def _strip_budget(budget: dict[str, Any]) -> dict[str, Any]:
    return {
        "amountRwf": budget.get("amountRwf"),
        "spentRwf": budget.get("spentRwf"),
        "usagePercent": budget.get("usagePercent"),
        "period": budget.get("period"),
    }


def _strip_goal(goal: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": goal.get("name"),
        "targetRwf": goal.get("targetRwf"),
        "currentRwf": goal.get("currentRwf"),
        "progressPercent": goal.get("progressPercent"),
        "monthsToGoal": goal.get("monthsToGoal"),
        "status": str(goal.get("status")),
    }


def _strip_dashboard(data: dict[str, Any]) -> dict[str, Any]:
    stripped: dict[str, Any] = {"totalBalanceRwf": data.get("totalBalanceRwf")}
    if "healthScore" in data:
        stripped["healthScore"] = _strip_health(data["healthScore"])
    if "budgetGauges" in data:
        stripped["budgetGauges"] = [_strip_budget(b) for b in data.get("budgetGauges", [])]
    if "spendingAlerts" in data:
        stripped["spendingAlerts"] = data.get("spendingAlerts", [])
    if "recommendations" in data:
        stripped["recommendations"] = data.get("recommendations", [])
    return stripped


def deep_remove_keys(obj: Any) -> Any:
    if isinstance(obj, dict):
        return {
            k: deep_remove_keys(v)
            for k, v in obj.items()
            if k not in _STRIP_KEYS
        }
    if isinstance(obj, list):
        return [deep_remove_keys(item) for item in obj]
    return obj
