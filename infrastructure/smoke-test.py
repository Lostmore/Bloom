"""Verify the real Gateway -> Identity -> Users flow after Compose startup."""

import json
import secrets
from urllib.error import HTTPError
from urllib.request import Request, urlopen


def request(method, path, expected, data=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    body = json.dumps(data).encode() if data is not None else None
    try:
        with urlopen(Request("http://127.0.0.1:8080" + path, body, headers, method=method), timeout=15) as response:
            status, content = response.status, response.read()
    except HTTPError as error:
        status, content = error.code, error.read()
    assert status == expected, f"{method} {path}: expected {expected}, received {status}"
    return json.loads(content) if content else None


phone = "+79" + str(secrets.randbelow(10**9)).zfill(9)
tokens = request("POST", "/api/v1/auth/register", 201, {
    "phoneNumber": phone,
    "password": secrets.token_urlsafe(24),
})
access = tokens["accessToken"]
profile = request("PUT", "/api/v1/users/me", 201, {
    "nickname": "Compose smoke test",
    "birthDate": "2000-01-01",
    "gender": "OTHER",
    "searchModes": ["FRIENDS"],
}, access)
assert request("GET", "/api/v1/users/me", 200, token=access)["id"] == profile["id"]
request("GET", "/api/v1/users/me", 401)
request("POST", "/api/v1/auth/logout-all", 204, token=access)
request("GET", "/api/v1/users/me", 401, token=access)
print("Gateway -> Identity -> Users registration, profile and revocation checks passed.")
