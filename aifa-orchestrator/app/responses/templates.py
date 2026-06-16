from typing import Any

from app.intent.classifier import Intent


def format_rwf(amount: int | float) -> str:
    value = int(round(amount))
    return f"{value:,} RWF"


def build_rule_based_reply(intent: Intent, engine_data: dict[str, Any]) -> str:
    if intent == Intent.AFFORDABILITY:
        return _affordability_reply(engine_data)
    if intent == Intent.SPENDING:
        return _spending_reply(engine_data)
    if intent == Intent.HEALTH_SCORE:
        return _health_score_reply(engine_data)
    if intent == Intent.BUDGET_STATUS:
        return _budget_reply(engine_data.get("budgets", []))
    if intent == Intent.GOALS:
        return _goals_reply(engine_data.get("goals", []))
    if intent == Intent.RECOMMENDATIONS:
        return _recommendations_reply(engine_data.get("recommendations", []))
    return _general_reply(engine_data)


def build_clarifying_question(missing_fields: list[str]) -> str:
    if "price" in missing_fields and "target_date" in missing_fields:
        return "To check affordability, tell me the item price in RWF and your target date."
    if "price" in missing_fields:
        return "What is the item price in RWF?"
    if "target_date" in missing_fields or "future_target_date" in missing_fields:
        return "By which date are you hoping to make this purchase?"
    return "Could you share a bit more detail so I can help?"


def _affordability_reply(data: dict[str, Any]) -> str:
    affordable = data.get("affordable", False)
    months_needed = data.get("monthsNeededMinimum", -1)
    projected = data.get("projectedSavingsAtTargetDateRwf", 0)
    confidence = data.get("confidence", "medium")
    warnings = data.get("warnings", [])

    if affordable:
        reply = (
            f"Based on your current savings pattern, you appear on track to afford this by your target date. "
            f"Projected savings: {format_rwf(projected)}."
        )
    elif months_needed > 0:
        reply = (
            f"At your current pace, this may take about {months_needed} month(s). "
            f"Projected savings by target date: {format_rwf(projected)}."
        )
    else:
        reply = (
            "Based on your recent data, this purchase looks difficult to afford by your target date "
            "without reducing expenses or increasing income."
        )

    if confidence == "low":
        reply += " This is an estimate — your income has been variable recently."
    if warnings:
        reply += f" Note: {warnings[0]}"
    return reply


def _spending_reply(data: dict[str, Any]) -> str:
    if data.get("status") == "building_profile":
        return "I need a bit more transaction history before I can analyze your spending patterns."
    categories = data.get("categories", [])[:3]
    if not categories:
        return "I don't see meaningful spending data yet for this month."
    parts = []
    for category in categories:
        name = category.get("categoryName", "Category")
        amount = category.get("currentMonthSpendRwf", 0)
        parts.append(f"{name}: {format_rwf(amount)}")
    alerts = [c for c in data.get("categories", []) if c.get("alertLevel") not in (None, "none")]
    reply = "Top spending this month — " + "; ".join(parts) + "."
    if alerts:
        reply += f" Alert: {alerts[0].get('alertMessage', 'unusual spending detected')}."
    return reply


def _health_score_reply(data: dict[str, Any]) -> str:
    if data.get("status") == "building_profile":
        return "Your financial health score will be ready after about 30 days of transaction history."
    score = data.get("score")
    band = data.get("bandLabel", "")
    driver = data.get("topDriver", "")
    improvement = data.get("topImprovement", "")
    return (
        f"Your financial health score is {score}/100 ({band}). "
        f"Top driver: {driver}. Suggested focus: {improvement}."
    )


def _budget_reply(budgets: list[dict[str, Any]]) -> str:
    if not budgets:
        return "You don't have active budgets set up yet."
    over = [b for b in budgets if b.get("usagePercent", 0) >= 100]
    near = [b for b in budgets if 90 <= b.get("usagePercent", 0) < 100]
    if over:
        b = over[0]
        return f"You're over budget on a category at {b.get('usagePercent', 0):.0f}% usage."
    if near:
        b = near[0]
        return f"A budget category is nearly exhausted at {b.get('usagePercent', 0):.0f}% usage."
    b = budgets[0]
    return f"Budgets look manageable. Example category usage: {b.get('usagePercent', 0):.0f}%."


def _goals_reply(goals: list[dict[str, Any]]) -> str:
    if not goals:
        return "You don't have any savings goals yet."
    active = [g for g in goals if g.get("status") == "active"]
    if not active:
        return "You have goals on file, but none are currently active."
    g = active[0]
    name = g.get("name", "Goal")
    progress = g.get("progressPercent", 0)
    months = g.get("monthsToGoal")
    if months:
        return f"'{name}' is {progress:.0f}% complete. At current pace, about {months} month(s) to go."
    return f"'{name}' is {progress:.0f}% complete."


def _recommendations_reply(recommendations: list[dict[str, Any]]) -> str:
    if not recommendations:
        return "No specific recommendations right now — your finances look stable based on recent data."
    top = recommendations[0]
    return f"{top.get('title', 'Recommendation')}: {top.get('message', '')}"


def _general_reply(data: dict[str, Any]) -> str:
    balance = data.get("totalBalanceRwf")
    if balance is not None:
        return (
            f"Your total balance is {format_rwf(balance)}. "
            "Ask about spending, budgets, goals, affordability, or your health score for more detail."
        )
    return "I can help with spending, budgets, goals, affordability checks, and your financial health score."
