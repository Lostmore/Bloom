"""Verify the real Gateway -> Identity -> Users flow after Compose startup."""

import json
import secrets
import subprocess
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


services = request("GET", "/docs/services", 200)
assert {"Bloom Identity", "Bloom Users", "Bloom Chat"} <= {service["name"] for service in services}
for name, path in (("identity", "/auth/register"), ("users", "/users/me")):
    specification = request("GET", "/docs/openapi/" + name, 200)
    assert specification["openapi"].startswith("3.")
    assert specification["servers"] == [{"url": "/api/v1"}]
    assert path in specification["paths"]
    assert not any(p.startswith("/internal/") for p in specification["paths"])
print("Live Identity and Users OpenAPI descriptions are available through Gateway.")

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

# This smoke test runs against a disposable CI stack: logout-all creates its first Identity event.
result = subprocess.run([
    "docker", "compose", "-f", "compose.yml", "-f", ".ci/compose.yml",
    "exec", "-T", "kafka", "/opt/kafka/bin/kafka-console-consumer.sh",
    "--bootstrap-server", "kafka:9092", "--topic", "bloom.identity.v1",
    "--from-beginning", "--max-messages", "1", "--timeout-ms", "30000",
], capture_output=True, text=True, timeout=45)
assert result.returncode == 0, "Identity Kafka delivery check failed"
event = json.loads(result.stdout.strip())
assert event["type"] == "sessions.revoked"
assert event["userId"] == profile["id"]
assert event["schemaVersion"] == 1
assert event["data"]["reason"] == "LOGOUT_ALL"
assert event["data"]["tokenVersion"] == 1
assert event["data"]["familyId"] is None
print("Identity outbox -> Kafka session revocation delivery passed.")
