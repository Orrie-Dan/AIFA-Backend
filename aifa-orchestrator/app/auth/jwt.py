import base64
import binascii
import json
import re
from dataclasses import dataclass


class AuthError(Exception):
    pass


@dataclass(frozen=True)
class TokenInfo:
    raw: str
    user_id: str
    user_hash: str


_BEARER_RE = re.compile(r"^Bearer\s+(.+)$", re.IGNORECASE)


def parse_authorization_header(authorization: str | None) -> TokenInfo:
    if not authorization:
        raise AuthError("Missing Authorization header")
    match = _BEARER_RE.match(authorization.strip())
    if not match:
        raise AuthError("Authorization must be Bearer <token>")
    token = match.group(1).strip()
    if not token:
        raise AuthError("Empty bearer token")
    user_id = extract_user_id(token)
    return TokenInfo(raw=token, user_id=user_id, user_hash=_hash_user_id(user_id))


def extract_user_id(token: str) -> str:
    parts = token.split(".")
    if len(parts) != 3:
        raise AuthError("Invalid JWT format")
    payload = parts[1]
    padding = "=" * (-len(payload) % 4)
    try:
        decoded = base64.urlsafe_b64decode(payload + padding)
        claims = json.loads(decoded)
    except (binascii.Error, json.JSONDecodeError) as exc:
        raise AuthError("Invalid JWT payload") from exc
    subject = claims.get("sub")
    if not subject:
        raise AuthError("JWT missing subject")
    return str(subject)


def _hash_user_id(user_id: str) -> str:
    import hashlib

    return hashlib.sha256(user_id.encode()).hexdigest()[:32]
