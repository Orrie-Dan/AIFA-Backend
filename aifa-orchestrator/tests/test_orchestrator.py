from datetime import date

import pytest

from app.intent.classifier import Intent, classify_intent
from app.intent.params import extract_affordability_params, missing_affordability_fields
from app.context.builder import anonymize_for_llm
from app.validator.output_validator import OutputValidator


def test_classify_affordability():
    assert classify_intent("Can I afford a laptop?") == Intent.AFFORDABILITY


def test_classify_spending():
    assert classify_intent("Show my food spending") == Intent.SPENDING


def test_classify_health_score():
    assert classify_intent("What is my health score?") == Intent.HEALTH_SCORE


def test_extract_affordability_params():
    params = extract_affordability_params(
        "Can I afford a 1,200,000 RWF laptop by December?",
        today=date(2026, 6, 16),
    )
    assert params is not None
    assert params.item_price_rwf == 1200000
    assert params.target_date.month == 12


def test_missing_affordability_price():
    missing = missing_affordability_fields("Can I afford by December?", today=date(2026, 6, 16))
    assert "price" in missing


def test_validator_rejects_hallucinated_number():
    validator = OutputValidator()
    engine = {"affordable": True, "projectedSavingsAtTargetDateRwf": 1500000, "confidence": "high"}
    result = validator.validate("You will have 9,999,999 RWF saved.", engine)
    assert not result.passed
    assert result.issue == "hallucinated_number"


def test_validator_accepts_engine_number():
    validator = OutputValidator()
    engine = {"affordable": True, "projectedSavingsAtTargetDateRwf": 1500000, "confidence": "high"}
    result = validator.validate("Projected savings are 1,500,000 RWF.", engine)
    assert result.passed


def test_anonymize_spending_strips_ids():
    data = {
        "status": "ready",
        "categories": [
            {
                "categoryId": "uuid",
                "categoryName": "food",
                "currentMonthSpendRwf": 50000,
                "merchantName": "Simba",
            }
        ],
    }
    stripped = anonymize_for_llm("spending", data)
    assert "categoryId" not in stripped["categories"][0]
    assert stripped["categories"][0]["categoryName"] == "food"
