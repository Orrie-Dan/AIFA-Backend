from enum import StrEnum
import re


class Intent(StrEnum):
    AFFORDABILITY = "affordability"
    SPENDING = "spending"
    HEALTH_SCORE = "health_score"
    BUDGET_STATUS = "budget_status"
    GOALS = "goals"
    RECOMMENDATIONS = "recommendations"
    GENERAL = "general"
    UNKNOWN = "unknown"


_AFFORDABILITY_PATTERNS = [
    r"\bafford\b",
    r"\bcan i buy\b",
    r"\bshould i buy\b",
    r"\bcan i get\b",
    r"\bpurchase\b",
]
_SPENDING_PATTERNS = [
    r"\bspending\b",
    r"\bwhere did my money\b",
    r"\bspent on\b",
    r"\bexpenses?\b",
    r"\bfood\b",
    r"\btransport\b",
    r"\brent\b",
    r"\butilities\b",
]
_HEALTH_PATTERNS = [
    r"\bhealth score\b",
    r"\bfinancial health\b",
    r"\bhow am i doing\b",
    r"\bhow am i doing financially\b",
]
_BUDGET_PATTERNS = [r"\bbudget\b", r"\bover budget\b", r"\bbudget status\b"]
_GOALS_PATTERNS = [r"\bgoal\b", r"\bsaving for\b", r"\bsavings goal\b"]
_RECOMMENDATIONS_PATTERNS = [
    r"\badvice\b",
    r"\brecommend\b",
    r"\bwhat should i do\b",
    r"\btips?\b",
]


def classify_intent(message: str) -> Intent:
    text = message.lower().strip()
    if not text:
        return Intent.UNKNOWN

    checks = [
        (Intent.AFFORDABILITY, _AFFORDABILITY_PATTERNS),
        (Intent.HEALTH_SCORE, _HEALTH_PATTERNS),
        (Intent.BUDGET_STATUS, _BUDGET_PATTERNS),
        (Intent.GOALS, _GOALS_PATTERNS),
        (Intent.RECOMMENDATIONS, _RECOMMENDATIONS_PATTERNS),
        (Intent.SPENDING, _SPENDING_PATTERNS),
    ]
    for intent, patterns in checks:
        if any(re.search(pattern, text) for pattern in patterns):
            return intent
    if re.search(r"\d[\d,.\s]*(?:rwf|frw)?", text):
        return Intent.AFFORDABILITY
    return Intent.GENERAL
