import re
from dataclasses import dataclass
from datetime import date, datetime

from dateutil import parser as date_parser


@dataclass(frozen=True)
class AffordabilityParams:
    item_price_rwf: int
    target_date: date


_PRICE_RE = re.compile(
    r"(\d[\d,.\s]*)\s*(?:rwf|frw)?",
    re.IGNORECASE,
)


def extract_affordability_params(message: str, today: date | None = None) -> AffordabilityParams | None:
    today = today or date.today()
    price = _extract_price(message)
    target = _extract_target_date(message, today)
    if price is None or target is None:
        return None
    if target <= today:
        return None
    return AffordabilityParams(item_price_rwf=price, target_date=target)


def missing_affordability_fields(message: str, today: date | None = None) -> list[str]:
    today = today or date.today()
    missing: list[str] = []
    if _extract_price(message) is None:
        missing.append("price")
    target = _extract_target_date(message, today)
    if target is None:
        missing.append("target_date")
    elif target <= today:
        missing.append("future_target_date")
    return missing


def _extract_price(message: str) -> int | None:
    candidates: list[int] = []
    for match in _PRICE_RE.finditer(message):
        raw = match.group(1).replace(",", "").replace(" ", "").replace(".", "")
        if not raw.isdigit():
            continue
        value = int(raw)
        if value >= 1000:
            candidates.append(value)
    if not candidates:
        return None
    return max(candidates)


def _extract_target_date(message: str, today: date) -> date | None:
    lowered = message.lower()
    month_names = {
        "january": 1,
        "february": 2,
        "march": 3,
        "april": 4,
        "may": 5,
        "june": 6,
        "july": 7,
        "august": 8,
        "september": 9,
        "october": 10,
        "november": 11,
        "december": 12,
        "jan": 1,
        "feb": 2,
        "mar": 3,
        "apr": 4,
        "jun": 6,
        "jul": 7,
        "aug": 8,
        "sep": 9,
        "oct": 10,
        "nov": 11,
        "dec": 12,
    }
    for name, month in month_names.items():
        if re.search(rf"\bby\s+{name}\b", lowered) or re.search(rf"\bin\s+{name}\b", lowered):
            year = today.year
            candidate = date(year, month, 1)
            if candidate <= today:
                candidate = date(year + 1, month, 1)
            return candidate

    iso_match = re.search(r"\b(20\d{2}-\d{2}-\d{2})\b", message)
    if iso_match:
        return date.fromisoformat(iso_match.group(1))

    try:
        parsed = date_parser.parse(message, fuzzy=True, default=datetime(today.year, today.month, today.day))
        return parsed.date()
    except (ValueError, OverflowError):
        return None
