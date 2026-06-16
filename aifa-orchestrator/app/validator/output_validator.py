import json
import re
from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class ValidationResult:
    passed: bool
    issue: str | None = None
    offending_value: float | None = None


_LOW_CONFIDENCE_QUALIFIERS = [
    "estimate",
    "variable",
    "fluctuated",
    "uncertain",
    "may vary",
]


class OutputValidator:
    def validate(self, llm_response: str, engine_data: dict[str, Any]) -> ValidationResult:
        numbers = self.extract_numbers(llm_response)
        allowed = self.build_allowed_set(engine_data)

        for number in numbers:
            if not self.is_allowed(number, allowed):
                return ValidationResult(
                    passed=False,
                    issue="hallucinated_number",
                    offending_value=number,
                )

        confidence = self._find_confidence(engine_data)
        if confidence == "low":
            lowered = llm_response.lower()
            if not any(q in lowered for q in _LOW_CONFIDENCE_QUALIFIERS):
                return ValidationResult(passed=False, issue="missing_low_confidence_qualifier")

        absolute_claims = ["definitely", "certainly", "guaranteed", "exactly", "precisely", "will be"]
        if confidence == "low":
            for claim in absolute_claims:
                if claim in llm_response.lower():
                    return ValidationResult(passed=False, issue="overconfident_claim")

        return ValidationResult(passed=True)

    def extract_numbers(self, text: str) -> list[float]:
        cleaned = text.replace(",", "")
        matches = re.findall(r"(?<!\w)(\d+(?:\.\d+)?)(?!\w)", cleaned)
        return [float(m) for m in matches]

    def build_allowed_set(self, engine_data: dict[str, Any]) -> set[float]:
        allowed: set[float] = set()
        self._collect_numbers(engine_data, allowed)
        expanded: set[float] = set()
        for value in allowed:
            expanded.add(value)
            expanded.add(round(value * 1.05, 2))
            expanded.add(round(value * 0.95, 2))
            if value >= 10:
                expanded.add(round(value / 100.0, 2))
        return expanded

    def is_allowed(self, number: float, allowed: set[float]) -> bool:
        if number in allowed:
            return True
        for candidate in allowed:
            if candidate == 0:
                continue
            if abs(number - candidate) / abs(candidate) <= 0.05:
                return True
        return False

    def _collect_numbers(self, obj: Any, allowed: set[float]) -> None:
        if isinstance(obj, bool):
            return
        if isinstance(obj, (int, float)):
            allowed.add(float(obj))
            return
        if isinstance(obj, dict):
            for value in obj.values():
                self._collect_numbers(value, allowed)
            return
        if isinstance(obj, list):
            for item in obj:
                self._collect_numbers(item, allowed)

    def _find_confidence(self, engine_data: dict[str, Any]) -> str | None:
        if "confidence" in engine_data:
            return str(engine_data["confidence"])
        for value in engine_data.values():
            if isinstance(value, dict) and "confidence" in value:
                return str(value["confidence"])
        return None


def strict_retry_prompt(user_message: str, engine_json: str) -> str:
    return (
        "Rewrite the answer using ONLY numbers present in the DATA block. "
        "Do not invent, estimate, or round beyond the provided values. "
        "Keep the response to 2-4 sentences.\n\n"
        f"[DATA]\n{engine_json}\n\n[USER QUESTION]\n{user_message}"
    )
